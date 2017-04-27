---
layout: post
title: Managing Aging Off Data and Schema Evolution
tags: schema, age off, ttl, big data, evolution
---

Fineo's architecture is designed to help people go faster, while having to do less by leveraging our NextSQL system. At the surface, its not that much different from the the [Lambda Architecture] - with a realtime serving layer and an offline batch step for offline analytics. However, we apply some "magic" to gracefully manage schema and provide a single interface for fast answers to realtime and offline queries.

In one of my early posts about Fineo I talked about how users had one month for formalize schema or some data would be lost. But that kind of sucks - you should never have to get rid of data.

Instead, I spent some time digging into the particulars of our query execution engine ([Apache Drill]) and our batch processing engine ([Apache Spark]) to enable reading data against typed columnar rows and schema-less JSON based rows.

The batch processing step looks like this:

1. Get all the data from periodic S3 dumps from the ingest pipeline (orchtestrated by Amazon Firehose)
2. Get all the known schemas for possible tenants
3. Apply schema to incoming rows
4. Group by tenant
5. Sub-group by schema-less and schema-ful
6. Write each group to own keyed directory hierarchy


# Schema-ful Rows

The schema-ful data are those columns of rows that have a known name. Fineo has a bunch of flexibility in assigning names of columns, but it is more or less a simple lookup of column name.

These rows can be made quickly searchable and suited to analaytics by collecting them into a columnar format (we chose [Apache Parquet]) and further optimized by storing them in a directory hierarchy suitable for time-series queries:

```
 /tenant id
  /year
   /month
    /day
```

Unfortunately, there is not a lot of inter-operability support between Apache Spark (batch processing engine) and Apache Drill (read engine). This means that writing partitioned data with Spark will not be readable by Apache Drill (this is because Spark writes partitions as key=value and Drill reads partitions as a nested hierarchy of value1,value2, etc.). That means we also have to extract the components of the timestamp and into the sub-partitions we want, construct those directories manually and write the Parquet data into the output directory.

A bit hacky, but it works.

# Schema-less Columns

Each well-formatted row is bound to a specfic 'metric type' (think user-visible table), but  schema-less columns are those columns but have not been formally added to the schema.

These kinds of fields occur when you have changing data and don't update the schema, either out of laziness or from a mispelling. In many systems, this unknown column will be a surprise and can either break your pipeline or get thrown out - bad outcomes either way. Fineo is built to handle this sort of change gracefully and without interruption.

These schema-less columns are grouped together in the batch processing step and written out to their own tenant-grouped, time-partitioned sub-directory:

```
/json
 /tenant id
  /year
   /month
    /day
```

This allows us to easily query either the JSON or Parquet formated data. At the same time, we can also periodically re-process the JSON data when there is a schema change to generate a columnar format, allowing faster reads of the data.

The trick here that binary data needs to be base64 encoded to support storage as JSON and then auto-translated back in the read pipeline. Not terribly efficient, but then works in all cases (oh the joys of platform!).

Finally, in the read process you need to join back the JSON and parquet rows on a per metrictype and timestamp basis. Naturally, this gets tricky if users start changing the data type of an schema-less column! But I'll save that for another post.

# Summary

Moving data to an offline, columnar storage allows us to efficiently support analytics queries and time-partitioning allows fast answers on a very wide range of events. But you can get that with a ton of existing systems (Hive, Kudu, etc.). The magic lies in how to support a fast-changing enterprise and dataset with features like column aliasing and renaming, and late/async-binding schema, so people can go faster, while doing less.


[Apache Drill]: https://drill.apache.org
[Apache Spark]: https://spark.apache.org
[Apache Parquet]: https://parquet.apache.org
[Lambda Architecture]: http://lambda-architecture.net/