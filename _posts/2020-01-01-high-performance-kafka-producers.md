---
layout: post
title: High Performance Kafka Producers
location: San Francisco, CA
subtitle:
tags: kafka, streaming, stream processing, big data
---

After my [Scaling a Kafka Consumer] post, it only seemed fair to take a dive into the producer side of the world too.
 It's got it's own set of problems and tuning fun that we can dive into.

# The setup

Let's assume that you already have a [Kafka] producer running, but its just not quite keeping up
with the data flowing through. This already means you are in the 95th percentile of users - 
generally the default client configurations are more than enough to work.

If you are interested in how the internals the Producer, I recommend taking a [look at this talk by 
Jiangie Qin](https://www.slideshare.net/JiangjieQin/producer-performance-tuning-for-apache-kafka-63147600) at 
LinkedIn. Not only does it walk you through how the Producer works, it can give you some first pass tuning 
recommendations. However, I prefer a bit more empirical evaluation based on what the client is telling us - its 
metrics - that you can take back to decide how to manage your particular use case.

# Back to basics

First, you need to understand _why_ your producer is going slow. So the first question we need to ask is, 
"Is it Kafka or is it me?"

Maybe its Kafka. Some things to check to ensure that the cluster is 'happy':
 * network handler idle time 
   * `kafka.network:type=SocketServer,name=NetworkProcessorAvgIdlePercent`
   * generally not below 60%, with average above 80%
 * request handler idle time
   * `kafka.server:type=KafkaRequestHandlerPool,name=RequestHandlerAvgIdlePercent` 
   * generally not below 60%, with the global average consistently above 70%
 * disks are idle
 * cpu usage is not maxed out (it shouldn't be if the above are true)

Unfortunately for us in this convenient - made-up -  story, Kafka seems to be idling happily along, so we are left with 
tuning  our client.
 
The obvious first starting place is ensuring that you have `compression.type` set. Compression on the producer side 
is seriously worth considering, especially if you have even a little bit of extra CPU available. Producer-side 
compression will help Kafka store more data quickly as the broker just writes the data to disk directly out of the 
socket (and vice versa for the consumer path), making it much more efficient for the whole pipeline if the a producer
can just handle the compression up front.

If you are running Kafka 2.X+, you should have access to `zstd` compression. Some tests I've seen show a marked 
improvement on the alternatives - its got  close to the compression of gzip, but with the CPU overhead of lz4. But 
your mileage may vary; be sure to test on your data! 
 
That out of the way, the next thing we should check to see is how good our batches are looking. The easiest 
configuration to tweak here is `linger.ms`. You can think of this as time-based batching. By increasing our latency, 
we can then increase our throughput by eliminating the overhead of extra network calls. 

For this, we should check out the `record-queue-time-avg`- the average 
time a batch waits in the send buffer, aka how long to fill a batch. If you are consistently below your `linger.ms`, 
then you are filling your batch sizes! So the first simple tweak is that we are going to increase our latency so that
 we can (no surprise!) increase the throughput too, by increasing your `linger.ms` (HINT: Kafka defaults to not 
 waiting for batches, leaning towards lower  latency producing, at the risk of more RPCs). I find `5ms` to be 
 a nice sweet spot.

Back to our toy example, you have set compression and tuned the `linger.ms`, but you are still not getting the 
throughput you need.

# Going deeper

Once you get further into the weeds, producer configurations start to get more inter-related, with some important 
non-linear and sometimes unexpected impacts on performance. So it pays to be extra patient and scientific about 
combinations of different parameters. Remember, we should be continually going back to understanding the root 
bottleneck while keeping an eye on optimizing the rate of records flowing through the Producer.

The next questions to ask are, how big are your records - as Kafka sees them not as you think they are - and are you 
making "good" batches?

The size of the batch is determined by the `batch.size` configuration - the number of bytes after which the producer 
will send the request to the brokers, regardless of the `linger.ms`. Requests sent to brokers will contain multiple 
batches, one for each partition.

So there are a few things we need to check on. How many records are there per batch, and how big are they? Here is 
where we can start really digging into the `kafka.producer` MBeans. The `batch-size-[avg|max]` can give you a good 
idea of the distribution of the number of bytes per batch. Then `record-size-[avg|max]` can give you a sense of the 
size of each record. Divide the two and tada! You have a rough rate of records per batch.

Now, you can match this to the `batch.size` configuration and determine approximately how many records _should_ be 
flowing through your Producer. You should also sanity check this against the `record-send-rate` - the number of records 
per second - reported by your producer.

 >  `<side note>`
 > 
 > So if you are struggling to fill your batches with the number of records, the problem now might not even be in your 
producer! It might actually be upstream in your processing - you did check to ensure that you were [Scaling a Kafka 
Consumer], right? It might as simple though as just increasing the amount of client threads, the parallelism, 
allocated to consuming records and passing them along to the producer. But let's assume you checked all those things.
 >
 > `</side note>`
 
You might be a bit surprised if you occasionally have very large messages (you did check `record-size-max` right?), 
as the `max.request.size` configuration will limit the maximum size of a request and therefore also inherently limit 
the number of record batches.

No, what about the time you are waiting for IO? Check out the `io-wait-ratio` metrics to see if you really are 
spending lots of time waiting for IO or doing processing. 

Now we need to make sure that your buffer size is not getting filled. Here `buffer-available-bytes`is your friend, 
allowing you to ensure that your `buffer.memory` size is not behind exhausted by your record sizes and/or batching.

Also make sure to check the bytes per topic metrics.

If you are producing to many different topics, this can affect 
the quality of the compression as you can't compress well across topics. In that case, you might need some 
application changes so that you can more aggressively batch per destination topic, rather than relying on Kafka to 
just do the right thing. Remember, this is an advanced tactic and you should only consider after benchmarking and 
confirming other things are not working. 

# Wrap up

Hopefully this will give you a bit more guidance than just the raw tuning documentation for how to go about removing 
bottlenecks and getting the performance out of your Producer that you know you be getting.

A summary of the configurations and metrics to tweak on the client:

 * `compression.type`
 * `linger.ms`
    *  `record-queue-time-avg`, average time a batch waits in the send buffer, aka how long to fill a batch
 * `batch.size`
    * determine records per batch
    * bytes per batch
       * see `batch-size-avg`, `batch-size-max`
    * records per topic per second
       * see `record-send-rate`
    * check your bytes per topic
 * `max.request.size`
   * can limit the number and size of batches
   * see `record-size-max`
 * time spent waiting for IO 
  * Are you really waiting? see `io-wait-ratio`
 * `buffer.memory` + queued requests 
  * see `buffer-available-bytes`
  * 32MB default, roughly total memory by producer, bytes allocated to buffer records for sending

Do you have any more suggestions? Drop a note in the comments below!

[Kafka]: http://kafka.apache.org
[Scaling a Kafka Consumer]: http://jesseyates.com/2019/12/04/vertically-scaling-kafka-consumers.html
