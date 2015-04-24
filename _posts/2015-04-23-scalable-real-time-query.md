---
layout: post
title: Scalable Real Time Query
location: San Francisco, CA
subtitle:
description: How do you manage a realtime queries and analytics over the same logic data?
tags: samza, kafka, zookeeper, real time query, analytics, log analysis, debugging
---

How do you manage a realtime queries and analytics over the same logic data?

# Disclaimer


1. a lot of the ideas here come from a [post](http://blog.confluent.io/2015/04/13/real-time-full-text-search-with-luwak-and-samza/) by Confluent, repackaged over software that exists today

2. I haven't actually built any of this - its merely a thought experiment to flesh out some ideas that have been circulating. Your milage may vary :)

# TL;DR
Real time queries get answered by a combination of a stream processor watching for matching events and a single lookup in a row store. Older and larger queries (e.g. roll ups) are served via a column-oriented store, which takes longer but works really well for analytics. There is some copied data, but its aged-off the row store to keep the row store fast and save space.

# The Setup

Say you want to query about logs that have the word “ERROR” in them from 2hrs ago, up to ‘now’. What you want to see is all the logs that exist at that time… but wouldn't be nice to see any updates for new log lines that come in? Thing about when you search twitter or scroll the facebook wall - that little blue bar telling you there are new updates?

That is the real-time query problem.

Historically, this is managed by a single query against your DB of choice to populate the initial results. Then periodically, you re-run the same query on the DB and just look for anything that occurred after the previous query.

This ends up being very costly and scaling poorly as you increase the number of queries on a partition.

# Digging In

Instead, imagine that the query can sit on the stream of incoming updates and only updated the queryer when there is a new document/record that matches the query[1]. Then you only need to do the historical lookup once and then register a listener any new updates that match your query.

Now, we can stick full-text search engine into a stream processor (say, Samza or Storm or Spark Streaming), but the question is how do we get the queries to the search nodes? The simple answer would be "put the queries into the stream as well!". However, for things like Samza, there is an [outstanding bug](https://issues.apache.org/jira/browse/SAMZA-353) that doesn't make this possible. To work across the stream processor of your choice, we can use Zookeeper as a realtime monitor of queries and a basic RPC/notification mechanism.

Ok, so now your stream processors as watching for new queries, each keyed to a query ID. Those queries get there from your endpoint of choice (lets go with, say, a web service!), which then:

 - listen on the bus for the messages keyed to the that ID
 - register the query in zookeeper
 
 When the user no longer cares about the query - they log off, you deregister the query and stop listening.
 
 Ok, so now you have a way of pushing out queries and getting updates from the log bus.
 
## Historical Lookup

Keeping with the stream processing everything (or the [Kappa Architecture](http://radar.oreilly.com/2014/07/questioning-the-lambda-architecture.html)) we would want to say that we just reprocess the historical data for documents that match our query and then stick them on the bus for the listening service.

The problem with this is you need to store all the events, but you don't necessarily know which queries are going to match. If you don't have more events than fit on a single machine, you are golden - just use a standard DB off you go. Maybe it even fits in memory across machines, then standard Spark can mange your query quite well.

Chances are, you probably dont, so queries are going to be expensive and you don't want to keep rerunnig them (this was a premise above).

Things like [HBase](http://hbase.apache.org) can help you cheat a bit on the time since you can filter by timestamp and it will only even read files that might have that time range. In the end though, you are still going to end up scanning a good bit of data.

This is where you need to start managing schemas. Backing with a NoSQL store means you can keep evolving schemas for the same type and still keep them all in the same table - you need a schema service to help you manage the different types[2]. When you have defined columns, you can then filter on just the ones that match your query and do very fast scans of a lot of data. These kinds of scans are still only good for looking a relatively small slice of the whole dataset (see you favorite query engine for the cross-over point for a single query engine vs. a bulk framework).

## Age-off and Parallel Query

The only problem with a row-oriented store is that its going to get slower or cost more as you scale out, when you add more data. In the end, you will still end up having to either read through more data off disk or go through more bandwidth - its going to hit a break point and just take _forever_.

You manage this by aging off data. You know your query pattern (for more real time queries) is going to mostly hit recent data - the stuff in your row store that you can access quickly and sequentially. Events older than a certain time - 2 days, 2 weeks, whatever - are removed (many systems can do this naturally, like HBase/Accumulo's TTL).

While you have been writing events from the bus into your row store (you have been reading off that same bus you are using to answer your real-time queries, right?), you also need to be writing into a column-oriented store. This gives you a way to keep data around after the age-off, but optimized for a slightly different access pattern (see Managing Analytics below).

# Managing Analytics

This is where a column-oriented store makes perfect sense. Anayltics don't care about being answered 'right now' (though faster is always better). Since we control who is sending the queries, we can direct those queries to the right source.

When you are updating rows in the column store, you are going to take a hit if you are frequently updating the same columns - you will be building up inreasingly large lists of source row IDs. Since we know we are managing immutable events, we take the standard tricks:
 
  - files are immutable and you just write events as you get them
   - leverage time range bounded partitions (e.g. one partition per week)
   
Updates now go away - when you get a new event, that state just gets written down (with the timestamp[3]) and you just use the new value for the columns you care about. Then when displaying the results you end up with nicely ordered historical changes.

So your analytics jobs (forked becuase the query asks about a large timerange or are doing rollups) just query the column store, leaving your realtime store alone; these kinds of queries on a row-oriented store would be very poor performing.

## Age-off Spanning Queries

For queries that span the age-off time, we can serve immediately queries from the row store and, as the user scrolls through, serve the remaining updates from the colum store. This is nice as the column store will not be interacting with changing data at this point, but instead be already just 'warm' - there are no in-flight new rows, those are all buffered in later time partitions - letting us have much lower contention on the access.


## Reconciling old updates
Common in some IoT architectures, like home power metering, you need to fix older events. In this case, you need to go and find the source file for the event and build up a 'modifications' file. This modification file is always also read in as you read in the data in the source file and is joined to the original data (modfications are assumed to be smallish) and reconciled to the new event. 

It would be nice to rebuild the original file so we don't need to keep around the modifications forever and to prevent modifications from growing too large. What's nice, is this kind of data is built to be processed in a fairly large batch (by something like MapReduce or Giraph). Thus, as we are building the result set from the client, we can fork-off a 'compaction' (to steal from HBase parlance) to rebuild the file.

This is not a novel idea - Hive does compactions already in MapReduce - but is nice in that it becomes just a normal part of the computation. Like with HBase compactions you will see increasing latency during compactions…but there is no magic :-/.

# Wrap Up

This kind of architecture is nice if you are looking to get rid of tools like Splunk that aren't able to handle the kind of scale your company needs.

There are a good amount of prerequisites:

 - log bus (e.g kafka)
 - stream processor (samza, storm, spark streaming)
 - metadata service
 - scalable row store
 - scalable column store

Currently, there is no open source solution that I've found that can manage schema as a service that works on a heterogenous data set (update in the comments if you find something that works!).

Chances are you probably already have the last two in some form. If not, just rolling out a standard row store will probably be fast enough to answer your real-time queries and a column-store will let you answer the big queries. 

What we are talking about here is how to manage all of them together without having to mess with extra bulk-processing for ingest and having **blazingly fast and useful** real time queries.

# Notes

[1] you can make this even faster using the ideas discussed in the [blog post](http://blog.confluent.io/2015/04/13/real-time-full-text-search-with-luwak-and-samza/) by leveraging Luwak to only search documents that might be used by your query.

[2] There is some work in Hive coming that kind of does this, but its a 'everything fits in Hive' model, rather than external service that you can plug into your own framework/tools. The problem with 'everything is Hive' is that, well, everything isn't Hive; the more you bend the model, the worse mechanical sympathy you get and then things get slower, and code becomes harder to read and reason about (basically, it all goes to shit).

[3] This is a bit harder to manage. Most column stores will just store the column value, so if you append the timestamp to every column value you barely have any matching values and a whole lot of bloat. What you need is tags per-cell (like cell-level tags in HBase/Accumulo) that are not used for comparison. Maybe this exists already, but if not it seems like a reasonable add to [your favorite column store here].