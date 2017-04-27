---
layout: post
title: Handling Errors in Fineo
tags: sql, error handing, architecture, fineo
---

Event error management not built into the Fineo platform originally (a big oversight), but we managed to add support for it over only a couple of weeks. Moreover, we were able to make it feel basically seamless with the existing platform.

When writing to Fineo, you can get an error if the ingest buffer is temporarily full. However, you may have written a bad event and that won't be detected until the event is processed a short time later. These errors can be seen in a special `errors.stream` table, but don't touch any part of the 'core' data storage layers.

<img src="/images/posts/managing-fineo-errors/write-architecture.png">

# Capturing Errors

An error can occur at each stage in the pipeline (currently only the raw processing stage and the write stage) and the message, context and event are captured and sent to a special AWS Firehose, while the event is marked 'successfully processed' in the parent buffer (avoiding attempted reprocessing).

Firehose will periodically export data to an S3 bucket, partitioned by year, month, day and hour.

When evaluating possible options for serving this data to users - AWS Athena, ElasticSearch - I decided to serve it back through the main Fineo API and query engine.

# Reading Errors

The key component of the query engine change was understanding that the `errors.stream` table pointed to a specific S3 parent directory and that, unlike the core Fineo architecture where the Tenant Key is part of the directory hierarchy, the Tenant Key is instead embedded in the event (and needs to be filtered so other tenants cannot access other tenant errors).

Queries then get translated quite simply:

```
> SELECT * FROM errors.stream
```

turns into

```
> SELECT * FROM errors.stream WHERE api_key = 'the_user_api_key'
```

for all requests.

Fineo uses [Apache Drill] under the hood, which natively supports reading JSON. It has the caveat that the JSON elements must not be comma-separated, but that actually works to our advantage as we don't need to worry about separating error events when sending them to Firehose. Drill also supports directory-based partitioning, allowing us to very efficiently zero in on a user's requested search timerange.

We then can easily read json data from the S3 storage engine with a hierarchy like:

```
/errors
 /stream
  /raw   <-- 'raw' stream processing stage
   /malformed  <-- the type of error in the stage
   /commit-failure
    /year
     /month
      /day
       /hour
        some-firehose-dump.json.gz
  /storage  <-- 'storage' stream processing stage
   ...
```

and even easily zero in on the type of error or when it occured.

<img src="/images/posts/managing-fineo-errors/error-reads.png">

The last bit came in adding support for the error table to the [Fineo Web App](https//app.fineo.io). While we support simple SQL query execution sent to a REST endpoint, I didn't want to overwhelm the web server. However, because of the nature of reading errors is inherently sequential in time, I could easily page through the results based on the last recieved time stamp. This meant we didn't have to leave open a socket connection or a cursor on the database.

# Summary

When you have a hammer, everything looks like a nail, but sometimes they _actually are nails_. With Fineo error management, we had a very clean integration with our existing infrastructure that let us implement a complete error analysis solution in under two weeks, from inception to deployment. And best of all? It fit well within our current user's mental model and solved their issues.

[Apache Drill]: https://drill.apache.org