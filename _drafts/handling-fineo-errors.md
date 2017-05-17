---
layout: post
title: Handling Errors in Fineo
tags: sql, error handing, architecture, fineo
---

Passing pipeline processing errors back to the user was not originally built into the Fineo platform (a big oversight). However, we managed to add support for it over only a couple of weeks. Moreover, we were able to make it feel basically seamless with the existing platform.

When writing to Fineo, you can get an error if the ingest buffer is temporarily full. However, you may have written a bad event and that won't be detected until the event is processed a short time later. These errors can be seen in a special `errors.stream` table, but don't touch any part of the 'core' data storage layers.

The overall Fineo platform looks like this:

<img src="/images/posts/managing-fineo-errors/write-architecture.png">

# Capturing Errors

An error can occur at each stage in the event processing pipeline (currently only two parts: raw and staged). The event, context and error messages are captured and sent to a special AWS Firehose, while the event is marked 'successfully processed' in the parent buffer (avoiding attempted reprocessing).

Firehose will periodically export data to an S3 bucket, partitioned by year, month, day and hour.

When evaluating possible options for serving this data to users - AWS Athena, ElasticSearch - I decided to serve it back through the main Fineo API and query engine. That meant extending the core to support this new data source, but was already had a SQL query engine that supported a partitioned, S3 storage mechanism; it seemed a lot easier that standing up a new service and learning a new set of infrastructure (and securing that too!).

# Reading Errors

In the 'regular' Fineo infrastructure, the Tenant Key is part of the directory hierarchy. Using Firehose meant that now the only place the Tenant Key is stored is the event itself. Thus our `errors.stream` table points only to a generic S3 'errors' directory, so the Tenant Key must be filtered out of the event itself (so tenants cannot access other tenants' errors). This ended up being only a slight shift in how we were already managing queries as the tenant key was already being surfaced as projected field from every event on read. Thus it came down to be only a simple matter of some query translation and correct redirection.

Queries get translated quite simply.

```
> SELECT * FROM errors.stream
```

turns into

```
> SELECT * FROM errors.stream WHERE api_key = 'the_user_api_key'
```

for all requests.

Fineo uses [Apache Drill] under the hood, which natively supports reading JSON. It has the caveat that the JSON elements must not be comma-separated, but that actually works to our advantage as we don't need to worry about separating error events when sending them to Firehose. Drill also supports directory-based partitioning, allowing us to very efficiently zero in on a user's requested search time-range.

We then can easily read JSON data with the S3 storage engine with a hierarchy like:

```
/errors
 /stream
  /raw   <-- 'raw' stream processing stage
   /malformed  <-- the type of error in the stage
   /commit
    /year
     /month
      /day
       /hour
        some-firehose-dump.json.gz
  /storage  <-- 'storage' stream processing stage
   ...
```

and even easily zero in on the type of error or when it occurred, and surface those as fields in the error event row.

<img src="/images/posts/managing-fineo-errors/error-reads.png">

Which will return a set of rows like:

| Timestamp | Stage | Type | Message | Event |
|-----------|-------|------|---------|-------|
| 149504314500 | raw | malformed | Event missing timestamp | {"metrictype": "metric", "value": 1}|
| 149504314502 | staged | commit | Underlying server rate exceeded. Retrying. | {"metrictype": "metric", "value": 1, "timestamp": 149504314502}|

The last bit of work came in adding support for the error table to the [Fineo Web App](https//app.fineo.io). While we support simple SQL query execution sent to a REST endpoint, I didn't want to overwhelm the web server. However, because of the nature of reading errors is inherently sequential in time, I could easily page through the results based on the last received time stamp. This meant we didn't have to leave open a socket connection or cursor in the database.

# Summary

When you have a hammer, everything looks like a nail, but sometimes they _actually are nails_. With Fineo error management, we had a very clean integration with our existing infrastructure that let us implement a complete error analysis solution in under two weeks, from inception to deployment. And best of all? It fit well within our current user's mental model and solved their issues.

[Apache Drill]: https://drill.apache.org