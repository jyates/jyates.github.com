---
layout: post
title: Why Kubernetes makes stream processing simple for Kafka
location: San Francisco, CA
subtitle: How to scalably ingest billions of events without going crazy
tags: kafka, etl, hadoop, hdfs, big data
---

* Stream processing is rapidly growing in popularity
* Many companies are scaling up rapidly and need good tools (often with very few people to run them - hard to find, expensive, etc).
* Best tools are the ones that are simple and consistently work
  * do one thing well
* Most cases do not actually need in-stream processing - its almost always trying to build interesting/cool things rather than wanting to work with/within solid primitives.
* Most data flows can be some combination of raw -> canonical -> data store, where the data store (or some downstream processing) handles the user-query complexity
* Many of these tools already exist at scale and work quite well
   * Presto, HBase/Cassandra/Dynamo, Hive, etc.
* All you then need is something to take data from Kafka and put into canonical form or into a datastore

So if we are thinking we have all these advanced cluster features for free, with a much more mature, battle-hardended system (and possibly even more importantly, at the right layer in the stack), then we just need to build an application that can dynamically scale up and down, be easy to understand and run super fast

* Kafka Connect claims to be this intermediate layer
 * simple to get started
 * quick to fall over
   * hard to manage many different applications - metrics, scaling, idempotentcy, isolation
 * makes a lot less sense when running in K8s
   * auto-restarts, isolation, affinity groups, namespacing
   * basically solves all the hard problems for you.
   * just left with lots of complexity to manage that starts to struggle at scale

Fortunately, we get most of that hard work for free from Kafka consumer groups. Now we just need some tools to make our transformations easy to describe, compose and test.

* Kafka Streams is other new kid on the block
  * does many many things - exactly once, local storage, fancy functions (joins, etc)
  * you probably dont need these features!
* akka-streams + kafka-akka is powerful tool kit
  * battle-hardened and proven to run at crazy big scale.
  * akka/actor model makes threads (basically) transparent, so you can just focus on business logic
  * ability to extend for your use case
  * lots of micro knobs for high perfomance or simple layers to tweak
    * adding a batch stage to keep your processor churning.
  * built-in DSL to make it super readable
    * beautifully composable pieces
  * functionally focused for simple testing
  * if you need more features they are generally already availably, e.g. akka-persistence for local storage

By allowing K8s to provide the cluster management functionality and some core akka libraries to handle multi-threading and load with Kafka, we are free to focus on just the core business logic. You can trust that when you need the scale, it will be there (at least, with some minimal tuning). This means you can deliver value faster than ever, and then trust that you will be able to sleep at night - no pagers are going off - because its built on a rock-solid foundation.
