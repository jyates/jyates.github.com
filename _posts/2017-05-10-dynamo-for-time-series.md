---
layout: post
title: Using DynamoDB for Time Series Data
subtitle: The gritty details of time management
tags: iot, dynamodb, big data, timestamp, smart car, aws, time series
---

Time is the major component of IoT data storage. You have to be able to quickly traverse time when doing any useful operation on IoT data (in essence, IoT data is just a bunch of events over time).

At [Fineo] we selected DynamoDB as our near-line data storage (able to answer queries about the recent history with a few million rows very quickly). Like any data store, DynamoDB has its own quirks. The naive, and commonly recommend, implementation of DynamoDB/Cassandra for IoT data is to make the timestamp part of the key component (but not the leading component, avoiding hot-spotting). Managing aging off data is generaly done by maintaining tables for a specific chunk of time and deleting them when they are too old.

Not unexpectedly, the naive recommendation hides some complexity.

# Overlapping Timestamps

At Fineo we manage timestamps to the millisecond. However, this can be a problem for users that have better than millisecond resolution or have multiple events per timestamp. Because we are using DynamoDB as our row store, we can only store one 'event' per row and we have a schema like:

| Hash Key | Range Key|
|-----------|---------|
|API Key, Table| Timestamp (ms)|

This leads us to the problem of how to disambigate events at the same timestamp per tenant, even if they have completely separate fields. If we were using something [Apache HBase], we could just have multiple versions per row and move on with our lives. Instead, we implemented a similar system with DyanmoDB's `Map` functionality.

# Leveraging Maps

Each write that comes in is given a unique hash based on the data and timestamp. The hash isn't a complete UUID though - we want to be able to support idempotent writes in cases of failures in our ingest pipeline. Instead, we get an id that is 'unique enough'.

For each row (`Api Key, Table | Timestamp`), we then have a list of ids. Each field in the incoming event gets converted into a map of id to value. Thus, to read an event from a row, you would first get the list of ids, then ask for that value for each ID in the map.

For example, suppose you had an api key 'n111' and a table 'a_table', with two writes to the timestamp '1', the row in the table would look like:

| Column | Value |
|--------|-------|
|apikey, table (range key)| n111,a_table|
|timestamp| 1|
| ids    | [1234,abc11]|
| field1 | {1234: "a", abc11: "b" }|

Where `1234` and `abc11` are the generated 'unique enough' IDs for the two events.

## Drawbacks

There are two major drawbacks in using this map-style layout:

1. DynamoDB has a max of 250 elements per map
2. Optimize for single or multiple events per timestamp, but not both

The first is a hard limt and something that we can't change without a significant change to the architecture. Fortunately, this more than fulfills our current client reqiurements.

The second comes from how DynamoDB handles writes. If we assume that there is generally only one event per timestamp, we can craft a request that creates the id list and column map immediately. If that fails, we could then attempt to do an addition to the column maps and id list.

Alternatively, we could attempt to update the column map and id lists, but if these lists don't exist, DynamoDB will throw an error back. Then we need to go and create the maps/list for the row with the new value.

Another valid approach would be to assume only one event per timestamp, and then rewrite the data if there is multiple events, but that leads to two issues:
 * handling consistency when doing the rewrite (what happens if there is a failure?)
 * multiple data formats on read, increasing the complexity

In the end, we decided to pursue a map-first approach. However, there is still the trade-off of expecting new timestamps or duplicate repeats; heuristics like "if its within the last 5 seconds, assume its new" can help, but this is only a guess at best (depending on your data).

Either write approach can be encoded into a state machine with very little complexity, but you must chose one or the other. On the roadmap is allowing users to tell us which type of data is stored in their table and then take the appropriate write path.

# Handling Time-to-Live

Our schema ensures that data for a tenant and logical table are stored sequentially. DynamoDB push-down operators (filter, scan ranges, etc.) allow us to quickly access time-based slices of that data on a per-tenant basis (e.g. we can go to the correct section because we know the hash key and the general range key).

However, DynamoDB can be expensive to store data that is rarely accessed. A common pattern is for data older than a certain date to be 'cold' - rarely accessed. It would be nice if the database automatically handled 'aging off' data older than a certain time, but the canonical mechanism for DynamoDB is generally to create tables that apply to a certain time range and then delete them when the table is no longer necessary.

**But what about data in the past that you only recently found out about?**

Its kind of a weird, but unfortunately, not uncommon in many industries. For example, with smart cars, you can have a car offline for months at a time and then suddenly get a connection and upload a bunch of historical data. This data is both old and new, ostensibly making it even more interesting than just being new.

To that end, we group tables both _by event timestamp and actual write time_. Since tables are the level of granularity for throughput tuning, and a limit of 256 tables per region, we decided to go with a weekly grouping for event timestamps and monthly for actual write times. This also fit well with our expectation of the rate data goes 'cold'.

### Event Time Prefix

We want to make it as fast as possible to determine the 'correct' tables to read, while still grouping data by 'warmth'. Since DynamoDB table names are returned in sorted order when scanning, and allow prefix filters, we went with a relatively human _unreadable_ prefix of ```[start unix timestamp]_[end unix timestamp]```, allowing the read/write mechanisms to quickly identify all tables applicable to a given time range with a highly specific scan.

### Write Time Grouping

Then we added on a description of the more easy to read month and year the data was written. This allows to find all the tables for which data was written a while ago (and thus, likely to be old), and delete them when we are ready. Because the deletion process is out of an any critical path, and indeed happens asynchronously, we don't have to be concerned with finding the table as quickly as possible. Instead, we can add the month/year data as a suffix to the event time range.

We can easily find the tables to delete once they are a few months old and unlikely to be accessed (and whose data scan still be served in our analytics organized offline store), while not accidentally removing data that is 'new and old'.

### Resulting Table Names

This gives us a _table name schema_ of:

```[start unix timestamp]_[end unix timestamp]_[write month]_[write year]```

which generates names like:

```
1491696000000_1492300799000_4_2017
1492300800000_1492905599000_4_2017
1492300800000_1492905599000_3_2017
```

A reasonable compromise between machine and human readable, while maintaining fast access for users.


# Summary

Since DynamoDB wasn't designed for time-series data, you have to check your expected data against the core capabilities, and in our case orchestrate some non-trivial gymnastics. On the whole DynamoDB is really nice to work with and I think Database as a Service (DaaS) is the right way for 99% of companies to manage their data; just give me an interface and a couple of knobs, don't bother me with the details.

However, in a timestamp-oriented environment, features databases like [Apache HBase] (e.g. row TTL) start to become more desirable, even if you have to pay a ingest throughput cost for full consistency. At the same time, events will likely have a lot of commonality and you can start to save a lot of disk-space with a "real" event database (which _could_ makes reads faster too). 

That said, managing IoT and time-series data is entirely feasible with Dynamo. There is a trade-off between cost, operations overhead, risk and complexity  that has to be considered for every organization. For Fineo, it was worth offloading the operations and risk, for a bit more engineering complexity and base bit-for-dollar cost.

Or you could just [use Fineo] for your IoT data storage and analytics, and save the engineering pain :)

[Apache HBase]: https://hbase.apache.org
[Apache Drill]: https://drill.apache.org
[use Fineo]: https://fineo.io
[Fineo]: https://fineo.io
