---
layout: post
title: Kafka Upgrade Validation
location: San Francisco, CA
tags: kafka, confluent, database, streaming, iot, upgrade
---

If you attended Kafka Summit, or followed along on Twitter, you probably heard many people mentioning that you really really ought to upgrade your Kafka installation. No surprise, it often will fix many obscure bugs (aka those you are guaranteed to hit at scale), while increasing performance and often times lowering operational costs. However, the big question is, "how can I be sure that this isn't going to break everything?" Related, is the additional question of, "what about bugs in the new version?"

I'll explore some of the process I recently went through when doing an upgrade of a somewhat out-of-date Kafka installation to the cutting edge stable release. Hopefully this can serve as a guide for doing your own upgrades, or at least help avoid some of the more common gotchas.

## Didn't someone else check this?

You might be asking, "why should I check for steady state bugs? Isn't that what the community does before cutting a release?" I would then remind you that:

> In theory, theory and practice are the same thing

Yes, there is some degree of validation by the community, but by definition this work is done on a volunteer basis, and is really just at best effort. In other communities, I've seen releases go out with huge severity bugs that would should have been caught by basic validation, but for one reason or another didn't.

In short, would you trust that your business critical infrastructure is safe based on volunteer work?

Not to say that the wonderful folks supporting the Apache Foundation projects are not often very high caliber, and doing amazing work - they are - but the risk just doesn't seem worth it to me.

Let's say though, that you don't use the vanilla open-source distribution, but some vendor's distribution. Now you might ask, "but certainly their validation is enough, right?"

And you would be right about many of the edge cases the standard validation might not catch. However, their test suites (hopefully automated!) also have risks in that they cover not necessarily the original code, but whatever patches the vendor has layered on top of the codebase. Now you have all the original code, but all the patches to validate, which is itself validated with code you also probably don't deeply know (if at all!).

Vendors are great for adding more trust the code, as well as finding/fixing bugs that might have crept into the edge releases.

However, there really is no substitute for doing the validation yourself - especially when you have millions of dollars (or more!) in business cost risk on the line.    

## Why Validate

Validation of a release will help you gain confidence that the bits you are pushing out will be "good". However, just as important, the validation will also help you gain confidence in the rollout process so that you have confidence not just in the final state, but also in every step along the way.

One of the biggest risks with new code is the risk of new bugs. While lots of work is done to validate the code, there is still substantial risks that are not likely to be covered by others. The most common are those related to your setup and usage: 

1. what does you particular _upgrade path_ look like and work
2. your particular usage (maybe you are using a less-common API and didn't know it?)
3. how things work on your particular hardware.

You would probably be surprised by the number of bugs that aren't found before a stable release. For instance, in upgrading to Kafka 2.2+ from 1.X,there are some major bugs like:

* KAFKA-8002 - Replica reassignment to new log dir may not complete if future and current replicas segment files have different base offsets
* KAFKA-8069 - Committed offsets get cleaned up right after the coordinator loading them back from __consumer_offsets in broker with old inter-broker protocol version (< 2.2)
* KAFKA-8012 - NullPointerException while truncating at high watermark can crash replica fetcher thread
* KAFKA-7165 - Error while creating ephemeral at /brokers/ids/BROKER_ID 
* KAFKA-7557- truncating logs can potentially block a replica fetcher thread, which indirectly causes the request handler threads to be blocked

These are non-trivial issues that impact two major areas: (1) data loss and (2) consumer offset loss. While data loss is understandably bad, the latter can actually be just as bad. If you have a lot of data retention for certain topics, loss of consumer offsets can cause your consumers to rewind themselves all the way back to the beginning of the topic, essentially crushing your cluster - now the brokers are thrashing your OS caches to support this old read, and also pushing data out as fast as they can. At the same time, if you have processes that don't expect very old data, this can break downstream components as well. Basically, it can be very very bad. 

As well as some more minor things, that might break your workflow:
 * KIP-272: added API version tag to metrics, which breaks JMX monitoring tools
 * KIP-225 changed the metric "records.lag" to use tags for topic and partition. The original version with the name format "{topic}-{partition}.records-lag" has been removed.
 * KAFKA-7373: GetOffsetShell doesn't work when SSL authentication is enabled

On top of that, there were a number of things that you need to take into account with major behavior changes:
 * Upgrading each broker can take lots of time as it rewrites the data on disk in the new format. This could leave partitions under-replicated for long periods of time
 * The default value for ssl.endpoint.identification.algorithm was changed to https, requireing you to set ssl.endpoint.identification.algorithm to an empty string to restore the previous behavior
 * ZooKeeper hosts are now re-resolved if connection attempt fails. But if your ZooKeeper host names resolve to multiple addresses and some of them are not reachable, then you may need to increase the connection timeout zookeeper.connection.timeout.ms 

Hopefully, by this point I've convinced you that you need to validate the code you deploy _before_ you deploy to production, even if it is a vendor release.

## How to validate

The first step should be to take a look at release notes (duh) for the version you are upgrading to, but also all the intervening versions. These will usually be a good start to make sure you have all the operational changes in place.

Then you should look to the JIRA for issues that are labels "critical" or "blockers", particularly for the version to which you are upgrading. Its up to you to determine if they are "real" issues and, if so, actually sever enough to warrant either your own fork or waiting for another release...or if its fine and you can go ahead.

From there, you can then start actually testing a release. For this, you will want to start by spinning up a completely separate cluster. We are going to be hammering on it.

### Tools

There are many tools available out there that can be used to validate and test Kafka. For instance, a couple of Google searches yields:

* Kafka Monitor - https://github.com/linkedin/kafka-monitor/wiki/Design-Overview
* ducktape - https://github.com/confluentinc/ducktape
* Jepsen - https://aphyr.com/posts/293-jepsen-kafka
* Pepperbox - templating + generating messages - http://pepperbox.gslab.com
* Blockage - docker-based network partition - https://blockade.readthedocs.io/en/latest/
* Gatling + kafka plugin - https://github.com/mnogu/gatling-kafka
* Kafka core ProducerPerformance - https://github.com/kafka-dev/kafka/blob/master/core/src/main/scala/kafka/tools/ProducerPerformance.scala

But what you really need to do is find the simpliest possible tool that will help you test the scenarios you are concerned about.

Personally, I've found [Kafka Monitor](https://github.com/linkedin/kafka-monitor) to be the most versatile tool, since automated failures, restarts, etc. seemed to be well covered in Confluent's existing test suite. We just really need to check how the consumers/producers view state in Kafka and that we are hitting our performance expectations, but don't need hooks into a month long running chaos suite.

KM is great in that it covers performance SLAs & data loss checks out of the box, and tracking consumer commit rate you can also check for consumer offsets being dropped.

The one thing I would have liked to see in Kafka Monitor is a consumer that you can turn on/off with an external REST call. This would helpful for ensuring in the face of consumer/broker restarts that only a couple of offsets are not being dropped. However, this is a relatively minor risk - as long as all the offsets weren't being dropped, a couple of messages being replayed is not a big deal.

### Methodology


If we want to understand how the new cluster will perform and operate, we need to start by baselining your existing installation. Start by standing up a small test cluster - minimum of 3 nodes, running hardware matching your production cluster - and deploying your existing version.

Then try and push as much data through as you can - produce and consume - with a single instance of the Kafka Monitor. We will call this the "continuous" instance/

Now, we are going to stand up a 2 other KM instances:
 1. stop/start (SS) - this instance will be bounced regularly, but retain it offsets in Kafka.
  * key configuration: `enable.auto.commit = true`, ensures that the consumer picks up where it left off
 2. stop/restart (SR) - this instance is also bounced, but will restart from the beginning of retention.

The single producer/consumer instance provides the data that all the consumers will use, and also validates the 'steady state' flow.
The SS consumer key use is that it ensures that consumer offsets are not lost. 
The SR consumer ensures that data is not lost.

Though we have this handful of consumers, the actual work will all be done by hand. 

We will start by deploying the new code to the brokers and then upgrading them one-by-one. With each broker restart we will also be restart the SS and SR consumers. Ideally, you don't restart them at the same point in the broker restart each time. For instance, if maybe right after you trigger the restart, or right after or after it has come back up.

There will be a number of restarts to bring the cluster up to the fully latest version. With Kafka you need a round of rolling restarts for each of:
 * running the new software
 * updating the interbroker protocol version (`inter.broker.protocol`)
 * update the client protocol version (`log.format.version`)
 
 This gives us plenty of opportunity to validate of data or offsets loss in via our consumers.

### Validations

So as we progress with this validation process, what do we want to check for?  

 ####  No data loss

All consumers should not show any data loss. This is actually a nice metrics that KM exposes and is based on the essentially validating that a "linked list" like structure is correctly linked for each partition.
 
#### Consumer offsets are not lost
  
When restarting the SR consumer, it should take about as long to go from the beginning of time, as every previous restart. For this, you will need to graph the offset commit-rate and compare it to previous restart steps. 

However, when restarting the SS consumer, it explicitly _should not_ go back to the beginning of the partition, but instead pick up where it left off. This is reflected as a roughly steady-state offset commit-rate, with a minor spike possible as it catches up to the producer.  

#### Performance

The SR consumer not only checks for data loss, but also allows us to validate the "top speed" of consumption - it is trying to pull data as fast as it can from the beginning of the topic. This allows us to get a handle on the comparative performance loss while progressing through each stage of the upgrade.

Additionally, our single producer should also be monitored to track its throughput throughout the upgrade process. It is expected to have slight hiccups when brokers restart, but at no point should the producer fail (be continuously unable to connect - indicative of a API compatibility bug), instead just needing to wait until the broker is ready to take writes again.

### Gotcha

To give yourself reasonable window of replay and validation, I've found its necessary have retention set to around 10 hours. This allows a wide enough window to validate the SR consumer's replay rate, but not keeping around so much that each step takes too long. That said, YMMV - that just seemed to be a nice number for our disks, network, etc.

Additionally, for this small three-node cluster, you want to ensure you set at least the following configs:
 * `acks = all`
 * `min.in.sync.replicas = 2`

Otherwise bouncing consumers can make it look like you are losing data, when in fact that is normal business operation of the restart.

## Approved Version

I've found that Confluent's 2.2.1-cp1 is quite stable and has back-ported patches to avoid the critical issues I've found when reviewing the stable Kafka releases. On top of that, the performance boosts, particularly over the 1.X and 0.10 lines is quite nice, as well as the solid JBOD support (making our lives much(!) better when dealing with the all too common disk failures).

On a small, 3 node cluster, running reasonably decent - but still commodity - hardware you could see as little as a 5% slowdown in producing and consuming during an upgrade.

Given that an upgrade will likely take you about 15min total per broker (assuming reasonably large volumes of data, 5min per restart and 3 restarts per step), you can then calculate approximately the amount of lag build-up in the process.

But, you won't take my word for it, right?
