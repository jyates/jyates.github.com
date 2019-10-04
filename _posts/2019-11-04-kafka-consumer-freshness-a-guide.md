---
layout: post
title: A guide to Kafka Consumer Freshness
location: San Francisco, CA
tags: kafka, consumer, latency, lag, big data, SLO
---

In my [recent talk at Kafka Summit](https://www.confluent.io/kafka-summit-san-francisco-2019/0-60-teslas-streaming-data-platform) I mentioned that users don't 
think in offsets, but rather in amounts of time - minutes, hours - that a consumer is behind. When you say, "We might
 have problem, your consumer is consistently 10,000 offsets behind," it would not be unreasonable to be met with 
 slack-jawed incredulity and/or glassy eyed stares. 
 
 However, users can easily inuit data 'freshness'. Were you to instead say, "We might have a problem, your consumer 
 is consistently 12 hours behind," you would quickly have a 
 productive conversation about whether that was actually a problem for their use case, how that might affect 
 downstream processing, etc. and if you are lucky actually turn what looked like a problem into lower operational 
 burden for you!  

 During my [Kafka summit talk](https://www.confluent.io/kafka-summit-san-francisco-2019/0-60-teslas-streaming-data-platform) I also mentioned we open sourced a tool - 
 the [Consumer Freshness Tracker] - that helps you translate from offsets (exposed by [Burrow]) into the amount of 
 time behind, the "freshness", of a consumer group.
 
 I'll explain the logic behind the Freshness Tracker and show how easy it is to run with just a short configuration.

# Motivation

As I mentioned, users think in time, not in offsets. When you tell them that their consumer is 1M offsets behind 
they have no context - is that a lot or a little? What is the latest data you _do_ have? And moreover, how long until it gets better? Offsets are what operators start out using - it is what Kafka exports as part of its metrics and what [Burrow] gives you as well.

And for a while that might be good enough. You can point users to historic dashboards where they can look up offsets for a topic-parition and map that to a time.

Definitely doable... but kinda lame.

# Existing Literature
 
From the [New Relic Kafkapocalypse article], they make the following definition:

> Commit Lag is the difference between the Append time and the Commit time of a consumed message. It basically represents how long the message sat in Kafka before your consumer processed it.

With this nice image:

<img src="/images/posts/kafka-freshness/new-relic.png">

> For example, you see that the Commit Lag of message 126, which was appended at 1:09 and processed at 1:11, is 2 seconds.
>  
>  Append Lag is the difference between the Append time of the latest message and the Append time of the last committed message. As you can see, the Append Lag for this consumer is currently 9 seconds.

Or explained slightly differently,

<img src="/images/posts/kafka-freshness/tesla-lag.png">

* commit lag = tc - t0
  * the time between an event entering and being committed, or the "latency"

* append lag = tN - t0
  * time between when the Log-End-Offset (LEO) and the latest commit message entered the topic

## Shortcomings of Commit Lag

Commit Lag at first blush seems like what we need. You are tracking how long an element takes to get committed. 
However, if your stream gets "stuck" (maybe bad code, maybe downstream problems), you will never hear about an issue 
because the latest offset never gets committed, so your commit lag cannot be updated yet! 


## Shortcomings of Append Lag

Append Lag does approximate Freshness when the stream is high volume, because we are (approximately) continually 
adding and and committing to a topic.

However, at low volumes Append Lag is wildly different from freshness.

For instance, if a message enters at t0 and then another event enters 6 hours later, the Append Lag will immediately jump to 6 hrs, as soon as the event joins the queue. 

Thus, Append Lag cannot be used ubiquitously for reliable alerting (though there may be value in determining when topics have issues upstream and are not receiving data).

# Deriving Freshness

You might be inclined to define Freshness as: 

> freshness = (current time) - (the most recent committed timestamp)

However, this is also incorrect - a consumer that is not receiving data will have a continually growing freshness.

For example, at t0 the event enters. At t1 it gets committed. Then 5 minutes later we calculate freshness, it will actually be

    5min + (t1 - t0)

Or the amount of time it between the calculation plus the time between the append and commit times. So if we check 10 minutes later, freshness would be

    10min + (t1 - t0)

So the freshness is then increasing even though no data is being added.

**This is certainly not correct**

## Intention of freshness

Going back to our definition, what we really want to know is:

> How far behind, in time, is my stream?

To answer that, we then need to calculate maximum amount of time an event has been in the topic/partition **without 
being committed**.

The oldest uncommitted data is always going to the (latest committed offset + 1); said another way it is the oldest, uncommitted offset.  Therefore, freshness is then

    freshness = (current time) - (append time of oldest, uncommitted offset) 
                  OR
                0, when no uncommitted offsets

Going back to the diagram above

<img src="/images/posts/kafka-freshness/tesla-lag.png">

Here t1 is the append time of the oldest, uncommitted offset.

If it is time (tc +1) but the topic has not added any more messages.
 
 * Freshness should be 0 (or, no lag)

If it is time (t1 + 1) > tc , so we have added a single message, but it has not been committed yet.

 * Freshness should be 1 (or, the amount of time that t1 has been in the topic without being committed)

If it is time (t1 + 10) > tc , so we have added a single message, but it has not been committed yet.

 * Freshness should be 10


# Building a Freshness Tracker

There is an existing OSS [freshness-like tracker available from Lightbend](https://github.com/lightbend/kafka-lag-exporter) that does some fancy tricks to avoid copying too much data and working around the 
lack of an offset-to-timestamp API in Kafka. Maybe this is for you, but this was (1) not available when we started, 
and (2) contains a premature optimization around minimizing the data being pulled from Kafka, leading to approximate 
answers.

Instead, the [Consumer Freshness Tracker] (CFT) is designed to be stupid simple and follow the Linux tools philosophy
 of composability. It takes the output of Burrow (allowing Burrow to focus on its job), then does the heavyweight 
 merge with state in Kafka to produce the amount of lag. The algorithm looks like this:

 1. Scrape consumers from Burrow
 2. For each consumer
     1. find the log-end-offset (LEO) and latest commit for each partition (as provided by Burrow)
     2. If there is no lag for that partition
        1. freshness is 0ms
     3. Else
        1. Read the read at the LEO from Kafka
        2. Get the timestamp from the record
        3. Freshness = current time - timestamp

Thus, any consumers that Burrow monitors, the CFT also tracks. This gives you one place to configure your white/black
 lists, helping your monitoring to always stay in-sync. 

The use of the LEO for each partition makes sense because it is definition, the longest amount of time between the 
latest committed offset and the oldest message not yet processed. Any newer messages have, by definition, a smaller freshness lag.

Because we also have the amount of time an offset sits in the queue before it is committed (offset time vs commit 
time), we also report the Commit Lag (from the first section), as a helper metric to understand the latency of an individual record in your stream.

And besides some multi-threading magic - for real,. production proven latency needs - that's it.

# Running a Freshness Tracker

You need to configure two main elements in the [HOCON configuration]: the Burrow URL and the clusters to query. For example, 

```
burrow:
  url: "http://burrow.example.com"

clusters:
  - name: logs-cluster
    kafka:
      bootstrap.servers: "l1.example.com:9092, l2.example.com:9092, l3.example.com:9092"
  - name: metrics-cluster
    kafka:
      bootstrap.servers: "m1.example.com:9092, m2.example.com:9092, m3.example.com:9092"
```

Any other clusters defined in [Burrow] will be ignored and only the consumers under the clusters we have defined here
 will be monitored. Everything under the `kafka` section is directly passed into the Kafka Consumer properties, allowing you to set SSL configs or tune the client as needed.

## Additional Tuning

There are a couple of additional tuning flags, which can be particularly useful when reading from clusters with very 
large records or when there are a large number of clusters, and you don't want to run multiple trackers with weird 
subsets of clusters or client configs  (though this is can often be the swiftest solution):
 * `workerThreadCount`
    * the number of concurrent threads querying Kafka, aka the size of the thread-pool used for polling
 * `numConsumers`
    * defined per-cluster, the number of Kafka Consumer instances to run. Each LEO pull will only happen on a single 
    consumer instance, so this is the max parallelism **intra-cluster** that you can expect.

These are also necessary because reading the offset record from Kafka to get the timestamp requires reading the 
entire record; there is currently no API to just get the key or the timestamp. This can lead to memory and/or latency
 challenges if you have particularly heavy-weight messages.

However, out of the box, the default configurations are likely more than sufficient.

# Happy Tracking

With the [Consumer Freshness Tracker] you have a small application that will convert your Consumers' offsets into freshness milliseconds, which is invaluable in providing accurate, and useful monitoring for users.

This has been running in production for nearly 1 year, so please do give it a shot in your environment - it just might change your entire mindset on monitoring. 

[Consumer Freshness Tracker]: https://github.com/teslamotors/kafka-helmsman/tree/master/kafka_consumer_freshness_tracker
[Burrow]:  https://github.com/linkedin/Burrow
[New Relic Kafkapocalypse article]: https://blog.newrelic.com/engineering/new-relic-kafkapocalypse/
[HOCON configuration]: https://github.com/lightbend/config/blob/master/HOCON.md
