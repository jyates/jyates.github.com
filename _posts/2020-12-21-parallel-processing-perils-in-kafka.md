---
layout: post
title: Stuck on Kafka
subtitle: Why getting paged at 2am is a feature, not a bug
location: San Francisco, CA
tags: kafka, bug, stream processing, big data, alpakka-kafka
---

Low volume data pipelines in Kafka tend to get stuck; there is more data to process but your consumers aren't moving forward. And its not because you are doing anything wrong in your application. In fact, it's part of the design!

A quirk in how Kafka manages its consumer groups can - without careful management after investigation into root causes (or just reading this post) - lead to 'out of order' commits that appear to cause a consumer group to become "stuck" at an offset. And if there aren't more messages to clear the clog, chances are you - the trusty data plumbers - are going to be woken up to fix it.

Fortunately, once we dive into understand _why_its happening, we can understand how to work around the issue. And even better, if you already use [Alpakka Kafka](https://doc.akka.io/docs/alpakka-kafka/current/home.html) you can get the fix for free, just by upgrading to 2.0.4+!

# From the outside

<img src="/images/posts/kafka-parallel-perils/bird-looking-in-rear-view-mirror.jpg">

When you are processing _more_ than a single Kafka message at a time - common in high throughput Kafka consumer applications - you are likely to see negative direction commits; that is, commits that go 'back in time' after another consumer committed forward progress! This can happen even if you are ensuring you have correct ordering of your offsets.

Which seems... impossible. At worst, a consumer _should_ only go back to the latest committed offset and then start committing from there after a rebalance.

Let's consider a case where you are processing a topic with two partitions, in parallel, on two different consumers: `c1` and `c2`. Their in-process data queues look like this:

	| Consumer| Partition Offset
	|   c1    | p1-5
	|   c2    | p1-5 | p1-6 | p1-7 |

`c1` is processing the message at offset 5 in partition 1 (p1) when a rebalance occured and p1 is moved to `c2`. For whatever reason, c1 is slow to commit its progress up to p1-5 (maybe you only flush commits every so often, maybe the processing got slow... it could be any number of things). When p1 is rebalanced to `c2`, it will start consuming from the latest committed offset (p1-4) and receives p1-5, p1-6 and p1-7 which it starts processing.

Recall that we are using a 'high throughput' application, so our consumers can work on these messages in parallel. A correct processing framework insures that we don't commit the progress out of order, even if we are done with the work; that is work can be done in parallel and even finish early, but it still needs to be committed in order (think a function like [akka's mapAsync logic](https://doc.akka.io/docs/akka/current/stream/operators/Source-or-Flow/mapAsync.html)). In-order commits ensure that data is always fully processed in the case of failures; in a failure the worst case then is reprocessing the data.

> The alternative to reprocessing (atleast-once message handling) is to use a transactional processing framework to ensure that you only ever process the messages exactly-once.
>
> However, that has its own overhead - transactions aren't free - so if you are looking for sheer throughput and velocity, you are often better off paying the occasional small price of reprocessing vs. the consistent tax of transactions.

In our example `c2` is compatively quite speedy, finishes its work and commits progress on p1 up to offset 7. Immediately after that, `c1` realizes that it needs to commit its progress and then it commits the work it has completed on p1, up to offset 5. Now the consumer state in the `__committed_offsets` topic looks like:

	| p1-4 | p1-7 |  p1-5 |


Uh oh! From the view of external monitoring (e.g. [Burrow](https://github.com/linkedin/Burrow)), the consumer looks like its 2 offsets behind. If this is a slow moving topic, one that doesn't get a lot of data, then this consumer could appear "stuck" like this for quite a while. In all likleyhood, its going to be stuck juuuuust long enough to page someone at 2am.

However, as far as the consumers are concerned, they are doing the right thing and everything is fine. That is, their internally reported lag will be zero, while the externally reported lag will be two. In this case, both are correct!

`c2` is correct in that is has processed all the data from p1 (up to offset 7), so it has no lag. And `c1` doesn't think its lagging because it is no longer assigned p1, so it doesn't report and lag from its internal metrics. But all is not well in the kingdom.

Let's say another rebalance were to happen right now. The newly assigned consumer would start receiving offsets starting from p1-5, as we see from the lag of two in Burrow. That consumer would then continue to make forward progress up to p1-7 and would then 'correct' the lag state.

This problem is only likely to page for these low volume topics/partitions - new data causes a 'forward' progress commit and state to recover. However, it can still cause wasted processing of messages that could be non-trivial in high-throughput environments; I've seen cases of **millions** of messages being reprocessed on _each rebalance_.

# From the inside

<img src="/images/posts/kafka-parallel-perils/looking-out.jpeg">
> Source: https://images.pexels.com/photos/3625023/pexels-photo-3625023.jpeg

It certainly seems like this is an issue with Kafka - we shouldn't be _allowed_ to commit progress for partitions that we are not assigned. But this is a **feature** of the low-coordination nature of consumer groups.

When a consumer group is created, it gets assigned a broker as the coordinator of the group. This gives the group a central place to manage state that all clients should be able to reach (all clients should be able to reach all brokers or you get really weird stuff happening, but not all clients need to be reachable from other clients in the same consumer group). The coordinator then helps manage which members of the consumer group are assigned which partition. When new members join or leave the group, the coordinator increments an 'epoch' and notifies all group members of the epoch change so they know to update their state.

Recall that the group coordinator is only a single broker, but the partitions storing the data are spread across potentially hundreds of Kafka brokers. Even for a small number of consumer groups, it becomes painful to coordinate the state of each consumer group (potentially thousands) across all the brokers. That means that Kafka brokers only care that a consumer is part of the latest epoch.

A corollary this is that Kafka brokers _do not care_ what a consumer is committing for a topic, as long as it has the correct epoch. That is, a consumer can commit a partition it has been not been (and never been) assigned. Meaning that an 'up to date' consumer - one with the correct epoch - can commit progress for any partition it so chooses.

The server-side architecture is designed to allow low coordination in increase the likelyhood of low-latency and high stability. However, that means a lot of the burden is placed on consumers to do the "right thing".


# Approaching a solution

<img src="/images/posts/kafka-parallel-perils/summit.jpeg">
> Source: https://www.humanedgetech.com/expedition/034tait01/images/P6040045.JPG

We _know_ that we should be relying on externally based metrics to monitor our systems; internal metrics are known to lie. However, in this case the external metric can be misleading - the data has been processed, but a rebalance would show the lag.

From experience, attempting to do a correlation between the internal and external metric and then modulating your alerts appropriately is a path fraught with issues. You are more likely than not to foot-gun yourself a number of times, trying to get the right correlation set up; slow reporting of internal metrics, acceptable deltas and window width are just a couple obvious gotchas.

Instead, we should go and fix the root cause - **consumers that are not assigned partitions should not be committing to them!**

If you are using alpakka-kafka (highly recommended as a Kafka stream processing library), then you should strongly consider upgrading to 2.0.4+ where I added [support for not committing unassigned partitions #1123](https://github.com/akka/alpakka-kafka/pull/1123). It solves this problem as part of the framework - yay, no need to change application code, it just works! - and ensures that (a) you never see this issue again and (b) get more sleep.

However, if you have your own home-grown system you will want to add filtering on the commit side to ensure that the consumer is still assigned the partitions. That means needing to track the assignments and correlate them with the state of the stream.

If your application keeps a buffer of data - ensuring you don't block on reading from Kafka - then keeping track of the assigned partition might have a double-win: you can filter out buffered messages that are no longer assigned the consumer, avoiding any extra processing at all!

[Kafka Streams](https://kafka.apache.org/26/documentation/streams/) continues to be exposed to this stuck commit problem - any time you are doing grouping, windowing, or in many stateful processing implementations, you get into asynchronous handling of message offsets. You are in a state where work is happening asynchronously to the commit, which can lead to progress being committed 'backwards'. As far as I know, this has not been addressed in open source.

Hopefully, you have seen the gory horror that is some of the guts of stream processing with Apache Kafka and understand why you might need to add special assignment tracking support to your applications. And if you don't, at least you have an explanation for why you are getting woken up at 2am.
