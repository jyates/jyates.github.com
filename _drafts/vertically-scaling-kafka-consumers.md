---
layout: post
title: Vertically scaling Kafka consumers
location: San Francisco, CA
subtitle: A tale of too many partitions; or, don't blame the network
tags: kafka, big data, mirror, consumer, partitions, hadoop, confluent
---

When scaling up Kafka consumers, particularly when dealing with a large number of partitions across a number of 
topics you can run into some unexpected bottlenecks. They get even worse when dealing with geographically remote 
clusters. The defaults will get you surprisingly far, but then you are left basically on your own.

Well, No More! Let's dive right in. 

# A real life example

Let's say you are mirroring data from an edge Kafka cluster into a central Kafka cluster that will feed your 
analytics data warehouse. You've setup the edge with 100+ partitions for many of the topics you are 
consuming (because you had the forethought to expect scale and knew partitions are generally pretty cheap - go you!).
 That means you could easily be mirroring 1000+ partitions into your central Kafka.
   
Let's add in that you are mirroring across the country because you are looking for geographic isolation as well as 
minimizing latency to getting data into a 'safe' system. That also means you have an extra 100ms of latency, roughly, 
for every mirror request you make.

Chances are, this isn't going to work out of the box. Too bad, so sad. Time to get engineering!

You might see something like
```
2019-06-28 20:24:43 INFO  [KafkaMirror-7] o.a.k.c.FetchSessionHandler:438 - [Consumer clientId=consumer-1, groupId=jesse.kafka.mirror] Error sending fetch request (sessionId=INVALID, epoch=INITIAL) to node 3: org.apache.kafka.common.errors.DisconnectException.
```

or, if you turn on, debug logging you might also see

```
2019-06-27 20:43:06 DEBUG [KafkaMirror-11] o.a.k.c.c.i.Fetcher:244 - [Consumer clientId=consumer-1, groupId=jesse.kafka.mirror] Fetch READ_UNCOMMITTED at offset 26974 for partition source_topic-7 returned fetch data (error=NONE, highWaterMark=26974, lastStableOffset = -1, logStartOffset = 0, abortedTransactions = null, recordsSizeInBytes=0)
```

What you consumer is really saying is, "I didn't get a response in the time I expected, so I'm giving up and trying 
again soonish."

Here are some quick configurations to check:
 -  default.api.timeout.ms
    - in older verisons of the client (pre 2.0) this controlled all the connection timeout
 - session.timeout.ms
   - how long until your consumer rebalances
   - watch the `join-rate` for all consumers in the group - joining is the first step in rebalancing.
 - request.timeout.ms
   - as of 2.0, how long the consumer will wait for a response
   - the logs are are great place to start here, to see if there are lots of failing   fetches
   - watch the broker metrics:
      - `kafka.server:type=BrokerTopicMetrics,name=FailedFetchRequestsPerSec` for a gut check of fetch statuses
   - watch the client metrics:
      - `fetch-latency-avg` and `fetch-latency-max` for latency when getting data
 - fetch.max.wait.ms
   - how long the server will block waiting for data to fill your response
   - metrics to watch on the broker:
     - `kafka.server:type=BrokerTopicMetrics,name=FailedFetchRequestsPerSec` for a gut check of fetch statuses
     - `kafka.server:type=DelayedOperationPurgatory,delayedOperation=Fetch,name=PurgatorySize` for the number of 
     fetch requests that are waiting, aka 'stuck in purgatory'
   - metrics to watch on the client:
     - `fetch-latency-avg` and `fetch-latency-max` for latency when getting data
 - fetch.min.bytes
   - minimum amount of data you want to fill your request
   - metrics to watch, both at the consumer level and the topic level
     - `fetch-size-avg` and `fetch-size-max` to see your fetch size distribution
     - `records-per-request-avg` for the number of messages you are getting per request
     - `fetch-latency-avg` and `fetch-latency-max` to ensure this is not causing you unexpected latency

(NOTE: all the metrics above are assumed client (consumer) side metrics MBeans, and have the prefix
` kafka.consumer:type=consumer-fetch-manager-metrics,client-id=([-.w]+)` or with `topic=([-.w]+)` for topic-scopeds 
metrics, unless otherwise noted)

These all can interact in interesting ways. For instance, if you tell the server to wait to fill the request, but then
 have the timeout set short you will have more retries, but potentially better throughput and likely saved bandwidth 
 for those high volume topics/partitions.

Don't forget that whatever you were using when connecting to a geographically more local source cluster (i.e. not 
across the country) will probably stop  working because  now you have an extra 50-100ms of roundtrip latency to 
contend with. The default settings, with 50ms timeouts for responses mean you will start to disconnect early all the 
time :)

Sadly, there are no explicit things I can tell you that will always work for these settings. Instead, they are a good
 guide to start reading the documentation and where to starting your fiddling. 

# What next?

So you have tuned your timeouts way up, made sure that you are fetching at least 1 byte...and still getting these 
errors in your logs. Its tough to pinpoint though...you might have a 100+ Consumer instances, and because they work 
as a team,  just one bad apple could tip you into perpetual rebalance storms.

Let's simplify the problem, by turning down the number of instances and chopping out some of these topics we 
need to mirror. Eventually you will probably get to a set of topics that suddenly starts to work! 

Hooray, things are working magically! Maybe it was those tweaks you made to get the topics working? Time to scale it 
back up...and its broken again. Crapola.

(this is _EXACTLY_ what happened to me)

Did you remember to check your GC monitoring? I bet you are going to find that your consumers are Stop-the-World 
(STW) GCing for near or over your timeouts. 

Your one (or two or three) little mirrors are GC'ing themselves to death; every time they disconnect, they generate a bunch 
more objects, which then add GC pressure. Even if your mirror starts working, it can quickly churn garbage
 and spiral into a GC hole from which it never recovers. This is even more frustrating as it can look like the mirror 
 is running fine for 10, 20 minutes and then suddenly - BOOM! - it stop working. 

I've found that using the Java GC options:

```
-server -XX:+UseParallelGC -XX:ParallelGCThreads=4
```

is more than sufficient to keep up. It doesn't use the fancy G1GC, but for a simple Mirror application, you don't 
need complex garbage collection - most object is highly transient and the rest are small and very long lived. 
Actually, a nice fit for the 'old' Java GC.

# Unbalanced Consumers

This can happen when you are consuming from multiple topics, but the topics don't have the same number of partitions.
 A quick reading of the documentation would have you think that it should just evenly assign partitions across all 
 the consumers, and it does...as long as you have the same number of partitions for all topics. As recently as Kafka 
 2.1+ (latest stable release I've tested), as soon as you stop having the same number of partitions the topic with the
 **lowest number of partitions** is used to determine the buckets, and then those buckets are distributed across 
 nodes.
 
 For example, say you have two topics, one with 10 partitions and another with 100, and 10 consumer instances. You 
 start getting lots of data coming into the 100 partition topic, so you turn up the number of consumers to 100, 
 expecting to get 90 consumers with 1 partition and 10 consumers to get 2 partitions; one partition on ten 
 instances for each of Topic One and then an even distribution of Topic Two.
 
 This is, unfortunately, **not** what you see. Instead, you will end up with 10 consumers, each with 11 partitions 
 and 90 consumers sitting idle. That's the same distribution you had before, but now with extra overhead to manage the 
 idle consumers!
 
 What you need is this configuration:
```
 partitioner.class = org.apache.kafka.clients.producer.RoundRobinPartitioner
```

Now the consumer group will round-robin assign the partitions across the entire consumer group. This will get you 
back to the distribution you expected, allowing you to nicely balance load and increase your overall throughput!

# Wrap Up

Hopefully, at this point you have all the tools you need to scale up your consumer instances. You know the basic 
tuning elements to check, have some guidelines to do basic GC tuning and finally the nice back-pocket trick to 
balance consumer groups when consuming from differently partitioned topics.
