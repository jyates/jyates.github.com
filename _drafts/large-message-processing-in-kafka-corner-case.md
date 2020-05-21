---
layout: post
title: large message processing in kafka corner case
location: San Francisco, CA
subtitle:
tags: kafka, big data, iot, streaming, stream processing
---

There is a huge gotcha when processing large messages in Kafka that you are probably missing. In the best case, it can cause you to waste years of CPU time and in the worst it can cause your processing to get unexpectedly 'stuck', delaying data and maybe even worse, waking you up at 2am to fix the issue. It all has to do with some Kafka internals logic and the nature of distributed systems. However, there are workarounds to help keep the data flowing and sleeping through the night.

Almost every Kafka operator has been woken up to deal with a 'stuck' partition. For some reason, the processor thinks it is up to date, but your monitoring believes that it is some number of messages behind. So what do you do? You bounce the consumer handling the partition, it finds the lag and continues along happily. Then you go back to sleep and write it off as a gremlin.

What you didn't realize is that this is Kafka operating exactly as it proposes to act! Its just that distributed systems are hard and corner cases can draw blood.

# Out-of-Order commits

 * Background on how consumers are grouped into epochs, preventing old consumers from interacting with current state
 * Any consumer instance with an up-to-date epoch can commit to any partition
 * Example
   * processing each message takes a longtime
   * message 10 for partition 1 is being processed on A
   * rebalance happens
   * message 10 for partition 1 is being processed on B
   * B completes before A, and even goes onto complete message 11 (its a fast message)
   * A then commits message 10 for partition 1
   * no more message arrive for the partition for a while
   * you get paged b/c it looks like partition 1 is lagging, when really it is up to date, it just got an out-of-order commit

TODO: diagram

# Avoiding rebalance pain

As of Kafka 2.3 the sticky assignor is safe to use (and became the default assignor in 2.4) thanks to the resolution of https://issues.apache.org/jira/browse/KAFKA-7026 via https://cwiki.apache.org/confluence/display/KAFKA/KIP-341%3A+Update+Sticky+Assignor%27s+User+Data+Protocol.

During steady state, a rebalance happens because a consumer got busy and missed its heartbeat. Generally, you can tune this away with `session.timeout.ms`, but there will be some subset of cases that still crop up. A common place this can cause lots of pain is when parsing big messages into their canonical form; this can lead to many many duplicate canonical messages downstream and cause pressure in Kafka and downstream data stores.

With the sticky assignor, a rebalance will only affect the consumer that was assigned the partition and the consumer that lost the partition. The rest of the partitions will generally remain in-place, so you cannot get another instance commiting to the 'wrong' partition.

# Reducing then Partition to Consumer ratio

Stuck partitions occur when you have a relatively low volume of data per partition, but many consumers to handle that volume. This could be necessary when processing large messages, but maybe it just an artifact of earlier load that has since migrated to a new topic or naturally been reduced. However, you are still left with this large number of partitions.

 * reduce the number of consumers handling the partitions
 * reduce the number of partitions
  * turn off producer
  * wait for consumers to consume all the data
  * delete and recereate the partition
  * not available to all use cases. some solutions include
    * coordinating with consumer/producer groups to migration to a new topic w/ fewer partitions
    * creating a 'logical topic' abstraction and using custom consumers/producer libraries

# Commmit Guard

real fix is to prevent these 'bad' commits from being madein the first place. 

  * Before processing: Check the partition of the message is still assigned to the consumer
  * Before committing: Check the partition of the commit is still assigned to the consumer

Alpakka-kafka added hooks for this metadata management, so you can track which partitions are assigned. Kafka Streams doesn't actually have this problem in the basic implementation - it processes one message at a time. However, if you ever introduce a buffer or parallel processing to increase the throughput you are opening yourself up out-of-order commit problems.
