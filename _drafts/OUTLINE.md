# Confluent Post: IoT Challenges and Techniques

## Problem
An overview of some of the common IoT processing challenges and then some techniques that you can apply to your problem.

Problem Statement
 - large number of devices
 - (some) low latency needs
 - NO data loss - every message counts
 - thundering herds
 - large messages
 - custom formats
 
## Basic Design

Before even deciding on technologies, the first question we should ask is, "what kind of properties do I need from my 
system", given the problem at hand. There are a number of things, but it seems like the key properties are:
 - durable storage
 - easy horizontal scalability
 - high throughput AND low-latency

The obvious choice here is Kafka - on top of fulfilling all of our requirements, it also is very stable, well supported
and has a great community. Obviously, is it not going to solve all of problems out of the box, but as long as we are 
careful in the design of the system around Kafka, we should be able to meet our needs for quite a while.
 
Let's start with the how we would setup the system. In front of the messages we definitely need to put an API - 
managing authentication to Kafka directly for millions of unique devices alone would be headache enough, much less 
trying to support the service protection we need. Great, now we have a place that we can do some simple routing and 
management of the events.

<Device to API image>

There are a couple of competing needs here; some messages are likely to be large, with lots of history to catch up, 
while some are going to be very important and need to be durable and available quickly, and some are just business as
 usual events. So, naturally, we need to have different mechanisms for processing each of them.
 
However, we have a a bigger problem first. With a large number of devices, we are likely to get stampeding herds for 
things like preferring to send data over wi-fi (which generally happens when people get home, which is generally right
around the same time). Our first priority should probably be making sure we land that data and confirm the success to
the client so they avoid having to re-send the data. In the case of some devices, either that have short-lived on-device
storage or are sending their "last gasp" data before they die, you might only have one chance to catch that data, so it
is often vitally important that the data land.
 
To not overwhelm any single partition, while continuing to be able to easily horizontally scale the easiest thing to 
do is to have a relatively random partitioning. The simple choice of key here is the epoch
millisecond that the message was _received_, giving you a reasonably equal distribution of data over even a few 
seconds of events. The advantage here is that we are helping to ensure that (a) we land data as quickly as possible 
while (b) helping ease operational burden and growth - we can just add more partitions and continue to scale horizontally 
without touching anything else in the system. The obvious disadvantage is that if a device is sending us 
multiple messages, it might make sense to handle them on the same consumer, depending on what state we need. For 
now, we will assume the latter is not a problem for the parsing stage. However, you could potentially borrow some of
the partitioning ideas we will discuss later here if that is a challenge in your environment.

## Parsing

Once we have our raw data, we need to make it usable. We could put the data directly into our databases or datalake, but 
you instead probably want an intermediate format so that you can make that streaming data available to other teams
across your organization. This also helps limit the complexity exposed to the rest of the organization as not everyone 
needs to be able to run their own raw data parsing when they read of the raw topic, while also helping ensure the schema 
in this parsed - "canonical" - topic remains backwards compatible and does not break consumers.

In any large organization, you are likely going to get many new logical data streams from devices. Either new devices are
added or new sensors are added to existing product lines or even new data is collected from existing devices (e.g. they
came equipped with hardware for which there was not firmware yet written to use and collect data on). 

Approaches:
 * one topic per stream type
 * one topic for all types, parse on the fly
 * one topic per device type (middle ground)
 
<Streams explosion image>

In reality, you are likely to see a mix of the possible routes to parsing. Additionally, you are likely to see a mix of
data formats (particularly if you have not transitioned to an enterprise-y "common data format"), which you will be responsible
for turning into usable, canonical data. So you are faced with a choice, do you (a) try to support all the possible formats, or 
(b) make the parsing pluggable. In the early days, you are likely more vertically integrated, so having the team handle
parsing the single format, might actually make sense to own the parsing for a handful of topics. Even at scale, owning 
some of the larger streams will help the team catch the corner cases and ensure the average case most people face works smoothly.

However, owning all the possible parsing does not scale long term. Beyond the "core" streams the team owns, moving to a
pluggable model is very helpful. 

You can provide some standard parsers (JSON, CSV, etc) are often quick to spin up and will solve many team's problems out of the box,
but definitely look at your common cases - and talk to your users! - before running off to implement standard tools. 

Formats and schema
 - provide simple interface to parsing
 - limit complexity exposed to users
 - avoid proliferation, focus on a couple of standard formats
   - if you are producing server-side, rarely a reason to not have schemaful messages

## Large Messages

Now let's look into the 'big message' problem. Anything over 1MB is going to start being challenging for 
Kafka, especially in a shared cluster that also needs things like low latency (though you can tune up to 20MB (0)). There are two main approaches we can 
take here: (a) chunking up the message into pieces and (b) storing a 'message reference' in Kafka to an external store. There
 are a number of posts written about each approach (1) (2) and even some projects to help us handle large messages natively
 (3). 
 
When you consider that man of the custom formats you will be using from the device are not easily splittable, a chunking 
approach starts to be more complicated - you need to maintain state _and_ need to ensure that chunks end up in the same partition, in the same
order (or keep even more state) regardless of retries. Additionally, given we have an ingest interface, its pretty easy to use the payload size to determine where/how to store the message - 
by value or by reference in an external store.  

Even with the additional overheads of multiple backends, an external store is starting to look pretty good. In the case 
of S3, there is actually relatively little operations overhead (outside of your initial setup). If you are already
on the cloud, S3 - or similar offerings from other vendors - is a nice choice not only for reliability & durability,
_and_ for being the defacto blob API (in case we want to move our implementation)_ as we can set a TTL on the bucket, so it will automatically cleanup messages that don't get 
delivered in failure cases. JESSE: awkward, review
 
Using an external store, we will have messages that come into our Kafka with a small wrapper which includes the data, or the reference, as well as some helpful things about the message:

  ```
  Message {
    string device_id;
    long arrival_time_millis;
    optional string reference;
    optional bytes body;
  }
  ```
while our event key (used for partitioning) is again, just the `arrival_time_millis`. Still pretty easy to handle when we
get to our parser.

## Big Messages are Slow

We should probably consider how to deal with those long-tail messages that are way bigger than your normal messages. For the most part, you will likely be
able to trust that the limits that your firmware developers specify match the limits the devices _actually use_ when sending
data to you. That said, devices are often known to go occasionally go a little bit insane, be it from cosmic rays, old
firmware or just bad luck. In this case, you could easily see a single message orders of magnitude larger than you expect.

How you handle that log will depend on your requirements, as well as the particulars of your parsing implementation. If the 
parser is super fast, then maybe just handling the big message is fine. For most cases though, its likely to cause some backup in the your
processing pipeline. That means, when ensuring you have at-least once processing for all messages, one of your partitions
will be blocked behind this sloooow message. With a couple of these, and some bad luck (aka. just a Tuesday, at scale), you
could easily start getting paged. And at that point, you really can't do anything but wait; increasing replicas - scaling up horizontally -
actually can rollback all the good progress your parser is making in getting through those big messages.

A coarse approach could be to just truncate these messages to the length that they _should_ be. And that might actually
work pretty well. If a device is going insane, the data from it is probably junk anyways, so its not worth your time
to process and store it.

But let's assume you want to keep all the data. One approach could be to parallelize the number of records you are 
parsing at the same time (remember you need to keep the
ordering on the completion though!). This can actually go a long way to minimizing the risks of the big messages. While
you are working on the first big message, you can in parallel be working on the next N messages, so when the big message
completes, you are also ready to commit those following messages. And if you have a number of big messages in a row, you are
dramatically reducing the user-visible parsing time, hiding it within your parallelization.

< Parallel parse - produce image >

The careful reader will recognize the risk here too - what happens when the consumer fails (or even just rebalances) when
parsing a large number of records in parallel? Well, you have to re-parse them because you weren't able to commit your
progress. One approach here would be to use some sort of distributed state to see if a message had been parsed, even if it wasn't
committed. However, state adds complexity, memory, and in many cases headaches you don't need. You could also buffer the progress
in memory, rather than sending the events downstream, but that could blow up the memory on your consumers, particularly if you are 
highly parallelized. Buffering also means that the small messages caught in this traffic jam don't make their way downstream in a
timely fashion, which could have implications if you prefer fresh data over a complete view of the data. I find that in practice, if you
have tuned your consumers appropriately, these failures/restarts are rarely frequent enough to make a material impact; you just 
let them happen and assume your downstream can handle the duplicate records.   

Another approach we could take is to get these "fat" records out of the way immediately. Instead of piping them into our
usual parsing pipeline, we can divert them to a 'slow lane' topic right at our API. Here you would just have these big logs, so you 
could run a separate set of the consumers that are specially tuned to process the logs more quickly, with less stringent
latency requirements (so you don't get woken up for normal business).

However, we can also be lazy about this decision and defer it to the stream itself. The parser is given a time-limit 
within which is would need to parse events from the message. If it doesn't complete in time (maybe with some wiggle room),
then it stops attempting to parse the message and just re-enqueues the message, this time in our 'slow lane' topic. This also
goes back to our original goal of landing data quickly, and offloading the work from the API (where there is more risk), to
the backend where we can be a little more leisurely and tolerant of things like retries.  

< slow lane api, slow lane re-direct image>


## High Priority Thunder

Thundering herds can easily occur with circadian rhythms or when doing an Over-The-Air firmware update (if you aren't careful).
However, in some cases the data being sent is absolutely critical to be received and then processed immediately. In that
case we have two clear options: (a) send the data to a different endpoint that goes to a high-priority topic or (b) send the parsed
data to a high priority topic JESSE: reword, this could be better. Essentially, in either case, we are creating a 'fast lane'
for the important data. Similarly you would want to set your alerting more rigorously for this processing as well, so that you
never fall too far behind.

 - partitioning canonical to group by
   - time bucket, UUID bucket

## Fleet Management

 - Meta consumer to a database/datawarehouse
   - quick answers about the global state of devices, message statistics, etc, coverage
 - separate meta from parsing
 - parsing outputs an additional "complete" record
  - producer doesn't care about topic too much, it just sends records
 

Looking forward
 - providing ownership to data owners to their own data flows
 - democratizing operational excellence
 - flow lineage and management

# References

 (0) https://kafka-summit.org/sessions/whats-inside-black-box-using-ml-tune-manage-kafka/
 (1) https://medium.com/workday-engineering/large-message-handling-with-kafka-chunking-vs-external-store-33b0fc4ccf14
 (2) https://www.slideshare.net/JiangjieQin/handle-large-messages-in-apache-kafka-58692297
 (3) https://github.com/linkedin/li-apache-kafka-clients

# Things to include, if we have more space
 - schema evolution considerations, you probably want forward
 - circuadian load smearing
 - k8s integration
 - pinned groups to limit bucket writes
	 - semi-dynamic partitioning such that consumer's figure out bucket to partition mapping and then use that to claim buckets in race
