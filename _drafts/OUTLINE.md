# Confluent Post: IoT Challenges and Techniques in Stream Processing

## Problem

At a far enough remove, IoT data streams can looks a lot like common webserver event streams - you have events being generated,
some times at high volumes, that need to be processed and either made available to downstream consumers or stored in 
databases. However, once you dig in, it turns out you have all the usual challenges and then a slew a new ones to content
with as well. Now, you have a large number of devices that have variable connectivity and a long tail of firmware versions, meaning you
can't just stop supporting some versions. At the same time, some of these devices can go 'insane' and start dumping piles
of data on your infrastructure, making it feel like a Denial-of-Service attack; with that long tail of devices, this DOS
starts to be come part of the business process and needs to be designed for up-front. 

Unfortunately, it gets even worse. Some of your data streams could be high priority - especially if you have devices that
are at all medical or health & safety related - mixed in with streams that are just then high volume 'normal operations' 
streams used by analysts for evaluating and understanding health and state of your fleet of devices. Often times, these streams have widely varying data formats, which
independently evolve on their own; as a stream processing focused team, you probably also don't have control over where or
when those changes happen. And the data formats on devices are not likely to be nice formats like Avro or Protobuf, either
because of CPU requirements and/or the desire to have more dense storage and transmission of data; hopefully they do continue to 
support at least versioning, if not compatibility guarantees.

Beyond that, you will also  need to add some basic fleet management and overview functionality. Since not all devices are regularly connected and can
have interesting bugs in firmware - 1 in a million is a daily problem at scale - you also need to build out data streams
just to understand the metadata about the state of your fleet of devices; whether the devices are healthy, reporting data,
creating weirdly large messages from being disconnected, etc. 

Taken all together, this can feel like a nearly insurmountable challenge. Fortunately, if you are facing some or all of these
kinds of problems, we have some techniques and approaches that can point you in the right direction.
 
## Basic Design

Before even deciding on technologies, the first question we should ask is, "what kind of properties do I need from my 
system." There are a number of things you might want to include, but for an IoT these are a nice set of base requirements:

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

There are a couple of competing needs here. Some messages are likely to be large, with lots of history to catch up. 
Others re going to be very important and need to be durable and available quickly. And other messages are just business as
usual events and can arrive late, but should never be lost. So, naturally, we need to have different mechanisms for processing each of them.
 
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
without touching anything else in the system.

<NEW: device -> api -> [sys_time, data] -> Kafka >

The obvious disadvantage is that if a device is sending us 
multiple messages, it might make sense to handle them on the same consumer, depending on what state we need. For 
now, we will assume the latter is not a problem for our parser. However, you could potentially borrow some of
the partitioning ideas we will discuss later here if that is a challenge in your environment.

## Parsing

Once we have our raw data, we need to make it usable. We could put the data directly into our databases or datalake, but 
you instead probably want an intermediate, "canonical" format of the data. Let's call this generic function from raw to
canonical format the 'parser'. The output of the parser - canonical data - is a single interface for all downstream operations. That makes it 
easier to build connectors to different backends. Additionally, limiting the functions of different tools allows you to limit
the complexity of each component in your system and allowing you to build a allowing you to build a nicely composable
stream processing framework. Its the Unix design philosophy, just applied at scale.

< raw to parser to canonical to ? image >

Another advantage of this canonical format is that you can make that streaming data available to other teams
across your organization. This helps limit the complexity exposed to the rest of the organization; not everyone 
needs to be able to run their own raw data parsing when they read of the raw topic, while also helping ensure the schema 
in this parsed - "canonical" - topic remains backwards compatible and does not break consumers. 

In any large organization, you are likely going to get regularly get many new data streams from devices. Either new devices are
added or new sensors are added to existing product lines or even new data is collected from existing devices (e.g. they
came equipped with hardware for which there was not firmware yet written to use and collect data on). 

There are a couple of approaches you can take to handle these new streams
 * one topic per stream type
 * one topic for all types, parse on the fly
 * one topic per device type (middle ground)
 
<Streams explosion image>

In reality, you are likely to see a mix of the possible routes to parsing. Additionally, you are likely to see a mix of
data formats (particularly if you have not transitioned to an enterprise-y "common data format"), which you will be responsible
for turning into usable, canonical data. So you are faced with a choice, do you (a) try to support all the possible formats, or 
(b) make the parsing pluggable. In the early days, you are likely more vertically integrated - having the team handle
parsing the single or limited number of formats - so the team owns the parsing for a handful of topics. Even at scale, owning 
some of the larger streams will help the team catch the corner cases and ensure the average case most people face works smoothly.

However, owning all the possible parsing does not scale long term. Beyond the "core" streams the team owns, moving to a
pluggable model allows you to scale and place the development & knowledge onus on the teams with a vested interest in
the data. Basically, saying "If you want the data so bad, then you have all the tools you need to get it." 

You can provide some standard parsers (JSON, CSV, etc) are often quick to spin up and will solve many team's problems out of the box,
but definitely look at your common cases - and talk to your users! - before running off to implement these standards. At the
same time, you will also likely start to have teams that produce server-side events on your platform as well. While the
common denominator is often just JSON, there is rarely an excuse for server-side teams to _not_ produce schema-ful messages; 
they have full control over all the message producers and there are libraries for the common formats in almost any
language they could want.

In a recent talk (4) I suggested exposing Parser interface like this:

```
parse(byte[]):: Iterator<Map<String, Object>>
```

You take in bytes (the message) and produce a number of events. The processing framework handles the rest of the magic of turning
those events into canonical messages, sending them along and committing progress.

This interface also has two failure modes - one that skips the message (it is known to be bad) and one 
that fails the message entirely (forcing the stream to retry). If your parsing is dependent on outside state - hopefully not! -
this allows it to retry and, if built properly, back-off from the external store. If not, then you get resilience to 
cosmic rays and a free retry. However, if the record really is busted in a way that the parse cannot understand, it eventually
blocks up the whole partition, and _should_ end up alerting the operations team. 
 
This interface pretty close to as generic as you can make a Parser interface; data comes in, messages go out. It is also something that users a can easily understand
and is surprisingly extensible, as we will see later.

By making the interface available to others you can start to bring some sanity to what the stream processing team manages
and what the firmware developers (that generate the events) have to worry about. Now, as the firmware wants to evolve
the data formats or add new streams, they have more power to control their own destiny, rather than waiting on the ingest team
to write and roll out new code.

Of course, there starts to be a dance around if the code should continue to support older
versions or if it is just easier to spin up a new topic and new parser for the changed data format. Remember, we have 
that long tail of devices so we still likely need to keep the old format around for quite a while; devices
can easily be online sending data but not get firmware updates, for many reasons. Its really up to you which burden you want
to bear - code complexity and overhead of differentiating formats or operational load of managing new topics and 
data parsing pipelines.  Here is important that the stream processing teams be intimately tied into this process (particularly for large changes), as it 
can affect not only the stream being scaled, but also the existing pipelines and thus, really, all the data the company
is getting from devices.

## Large Messages

Now let's look into the 'big message' problem. Anything over 1MB is going to start being challenging for 
Kafka, especially in a shared cluster that also needs things like low latency (though you can tune up to 20MB (0)). There are two main approaches we can 
take here: (a) chunking up the message into pieces and (b) storing a 'message reference' in Kafka to an external store. There
 are a number of posts written about each approach (1) (2) and even some projects to help us handle large messages natively (3). 
 
When you consider that man of the custom formats you will be using from the device are not easily splittable, a chunking 
approach starts to be more complicated - you need to maintain state _and_ need to ensure that chunks end up in the same partition, in the same
order (or keep even more state) regardless of retries. Additionally, given we have an ingest interface, its pretty easy to use the payload size to determine where/how to store the message - 
by value or by reference in an external store. It also gives us the flexibility to managing our partitioing to support
different use cases without extra mental overhead of remembering to handle chunking; all the streams can look the same,
regardless of priority, message size or volume. 

Even with the additional overhead of multiple backends, an external store is starting to look pretty good. If you are already
on the cloud, S3 - or similar offerings from other vendors - is known for reliability & durability, with little operational
overhead. Further, its also the defacto blob API, in case we want to move our implementation off a given vendor or on-premise. 
S3 also has the nice feature of allowing us to set a TTL on the bucket, so it will automatically cleanup messages that don't get 
delivered in failure cases.
 
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

A long tail doesn't just apply to the frequency of seeing a device or number messages from it, it also applies the size of
individual messages. For the most part, you will likely be
able to trust that the limits that your firmware developers specify match the limits the devices _actually use_ when sending
data to you. That said, hardware devices are often known to go occasionally go a little bit insane, be it from cosmic rays, old
firmware or just bad luck. In this case, you will regularly see single messages orders of magnitude larger than you expect.

How you handle that log will depend on your requirements, as well as the particulars of your parsing implementation. If the 
parser is super fast, then maybe just handling the big message is fine. For most cases though, its likely to cause some backup in the your
processing pipeline. That means, when ensuring you have at-least once processing for all messages, one of your partitions
will be blocked behind this sloooow message. With a couple of these, and some bad luck (aka. just a Tuesday, at scale), you
could easily start getting paged. And at that point, you really can't do anything but wait; increasing replicas - scaling up horizontally -
actually can rollback all the good progress your parser is making in getting through those big messages.

A coarse approach could be to just truncate these messages to the length that they _should_ be. And that might actually
work pretty well. If a device is going insane, the data from it is probably junk anyways, so its not worth your time
to process and store it.

But let's assume you want to keep all the data.

You could try to parallelize the number of records you are parsing at the same time (remember you need to keep the
ordering on the completion though!). This can actually go a long way to minimizing the risks of the big messages. While
you are working on the first big message, you can in parallel be working on the next N messages, so when the big message
completes, you are also ready to commit those following messages. And if you have a number of big messages in a row, you are
dramatically reducing the user-visible parsing time, hiding it within your parallelization.

< Parallel parse - produce image >

The careful reader will recognize the risk here too - what happens when the consumer fails (or even just rebalances) and it is
parsing a large number of records in parallel? Well, you have to re-parse them because you weren't able to commit your
progress. One approach here would be to use some sort of distributed state to see if a message had been parsed, even if it wasn't
committed. However, state adds complexity, memory, and in many cases headaches you don't need (stateless is already hard enough).
 
You could also buffer the progress in memory, rather than sending the events downstream, but that could blow up the memory on your consumers, particularly if you are 
highly parallelized and handling large messages. Buffering also means that the small messages caught in this traffic jam don't make their way downstream in a
timely fashion, which could have implications if you prefer fresh data over a complete view of the data. I find that in practice, if you
have tuned your consumers appropriately, these failures/restarts are rarely frequent enough to make a material impact; you just 
let them happen and assume your downstream can handle the duplicate records.   

Another approach we could take is to get these "fat" records out of the way immediately. Instead of piping them into our
usual parsing pipeline, we can divert them to a 'slow lane' topic right at our API. Here you would just have these big logs, so you 
could run a separate set of the consumers that are specially tuned to process the logs more quickly, with less stringent
latency requirements (so you don't get woken up for normal business).

We could make the slow-lane choice up-front when receiving the records in our API layer. However, that puts our goal
of landing data durably and quickly at risk as we load more and more logic into our API. Instead, we can also be lazy 
about this decision and defer it to the stream itself. 

The parser is given a time-limit  within which is would need to parse events from the message. If it doesn't complete in time (maybe with some wiggle room),
then it stops attempting to parse the message and just re-enqueues the message, this time in our 'slow lane' topic. This
helps keep our API simpple and offloads to the backend where we can be a little more leisurely and tolerant of things like retries.  

< slow lane api, slow lane re-direct image>

It really all depends on what you determine your most important requirements are.

## High Priority Thunder

Thundering herds can easily occur with circadian rhythms or when doing an Over-The-Air firmware update (if you aren't careful).
But in some cases it is absolutely critical that the data land and be processed quickly. In that
case we need to have a 'fast lane' for this data, either with a raw topic or a 'canonical' topic that re-directs from your
generic catch-all topic. Similarly you would want to set your alerting more rigorously for this processing as well, so that you
never fall too far behind.

< fast lane image >

Adding a separate endpoint in your API doesn't add a ton of complexity and can help get your data landed and processed with
its own dedicated set of parsers quickly. 

However, you might have a competing need from your backend that data from devices be somewhat contiguous. This can
help when, say writing to a database and not wanting to spray writes from each consumer to all of your shards. That means
we need to organize our canonical topics a bit more intelligently, even for these high-priority topics.
 
For some of the streams you could probably get away with just using a relatively even event distribution on something like the 
`event_millisecond` - the timestamp of the event itself, rather than when the message was received.
 Once passed through Kafka's partition hashing, it is very unlikely to see anything but approximately the same number of messages per-partition,
even if the one device does go 'insane', it is still likely (hopefully!) to produce an event stream with an increasing
event time.

But we were talking about the case where you _can't_ just use the event time, and need to be a bit smarter and gives you
more continuity. A simple approach
to get contiguous event streams is to just partition on devices' UUID. This ensures that the data for one device always
ends up going to the same partition and, retries and restarts aside, in the same order it came in as. This can make life
very easy for downstream consumers.

At the same time, it can also make life positively horrible when you get stampeding herds of a handful of devices. This
could be a normal business operation, like devices getting sync'd on circadian rhythms and only sending data on wifi (e.g.
when people get home). Alternatively, it could be a sort of 'last gasp' before the device fails to ensure that all the
diagnostic information necessary is available, but that could easily be a big flood on its own.

Either way, a consistent UUID based partitioning scheme is unlikely to succeed, especially with smaller fleets of devices. That said, it can work with large fleets,
but this often needs to be in the millions before it is viable and you are going to see the same problems with rollouts, new
data streams and new device types, so we can't count on sheer scale here to smooth out our canonical topics.

Stepping back, either for money savings or efficiency reasons devices are unlikely to be constantly connected and sending
a stream of events. That means you are going to see discretized event windows per message. So we can map these chunks into
time windows that are convenient for our storage system. Now our canonical topic's partition key can be something like UUID + `time_bucket`, where
the bucket is dynamically generated from the event time.
 
< wrapper (not the system time) -> event blob -> event + timestamp -> this timestamp >

< mapping timestamp to hourly time buckets> 

Now when we read from the canonical topics, we continue to get devices' data grouped together for large swathes of time,
but avoid the risk of DOS-like data floods. This can be incredibly useful for streams that have to be processed quickly, but
are also the ones most susceptible to the thundering herd risks.

## Fleet Management

When you have a large fleet of devices to manage and analyze, it becomes important to understand how much of the fleet you
are really getting for any given day. Depending on circadian herding or firmware configurations, you could see 90% of the 
fleet daily or 30% of the fleet daily. And if that percent drops off suddenly, you will want to know that too.

< long tail 90% or long tail 35% >

This is where the use of a 'metadata' stream is very powerful. Its built off the same raw stream that we used for the 
standard raw to canonical parsing, but just tracks some basic things about the events without doing a full parse of each message.
This could include things like device type, device UUID, firmware version, time received, source topic, message size and
 maybe message start/end times. 

Once you have this data you can do vey powerful queries helping you understanding the state of the fleet - how much
of the fleet do we get data for in the last day, two days, week? What devices have we never gotten data or are chronically
late? How big are the messages we are getting? The list goes on.
 
The key though, is that the metadata stream is primarily generated separately from the standard canonical event records from
the messages. The work you are doing is much smaller, both in terms of CPU and data generated, so it relatively cheap to
add, but also makes answering your big questions fast and cheap too. For instance, determining the relative coverage for
every device in your fleet could mean scanning petabytes to just find a couple of devices, but the metadata could run just
tens of gigabytesm, even for a large fleet over many years.

< raw to canonical and metadata >

Notice I said that the metadata stream should be primarily separate, not entirely separate. We have a nice opportunity in the
raw to canonical parsing step to output some additional metadata metrics; metrics we could only know post-parsing about the
messages that give us a more complete  understanding of the fleet and the data we are getting. Some of the things we could track include number of events parsed,
time to parse, and if the parsing was successful. 

< raw to canonical/meta, raw to metadata >


What is neat about the Kafka producer is that it doesn't get pinned to one destination topic - you can give it messages for 
multiple topics and it will happy pass them along. And since our metadata about the record is relatively small compared
to the event stream, it is approximately "free" for us to add this additional metadata about the parsing itself. 

This small amount of additional metadata can be very impactful. Now you can answer questions of the quality of the data
being sent from the devices - how often is it well-formed, how good is the custom format's compression in practice? It 
can also inform things like the amount of parallelism you want to employ for large messages because you can understand in
real time how long are you spending parsing per message and then make informed decisions around throughput vs. data
duplication during restarts.

All in all, metadata streams are a powerful tool help you understand how your fleet and how your stream processing is 
operating, for relatively cheap. It doesn't cost you a lot of data storage or CPU for processing. You do pay an operational
overhead for managing an additional stream, but I would argue you get that back in spades from operational gains for the
fleet and understanding the stream itself.   

## Looking forward

We've talked about a number of different topic types, fast lanes and slow lanes, raw topic and canonical topics. 
You can also mix and match any/all of these pieces to achieve your business goals and requirements. However, its also
very easy, in the process, to build out a horribly complex set of data flows, where you get fan-out and fan-in, where you 
build a system whose end-to-end SLOs are unreliable or impossible to calculate. Unfortunately, there is really no good, 
readily available tools for managing all this complexity (Amundsen (6) looks like a promising start). I've built simple command-line tools to help developers visualize
these flows, but you often end up relying on hand-crafted diagrams that can quickly go out of date. It's really a far cry from what we would want to make available to end users.

Because, at the end of the day, managing these data flows really shouldn't be the goal of the teams building out these tools
and pipeline components. It's the end-users, whose firmware generates the events and the analysts who look at those events 
that really need to be empowered to build and manage these pipelines. And let's be honest, these people don't want to deal
with debugging, provisioning and scaling these pipelines out. That should be the province of the data teams.

So now we have this nice interface between teams that revolves around the idea of scaling operations. You let the 'external' 
developers write code (in this case, often just a parser) and wire things together, while data pipelines team 
manages scaling not only the software but _scaling the operations_ of the pipelines. And that is really 
where the power of the cloud lies; its about scaling the operations outside of the team to the rest of the organization. Users
get to say 'take this data, so this to it and put it here' and the tools should just make it so, without any more overhead. The
streaming teams power this infrastructure and make sure it can scale as needed (usually by continuing to own some big streams
a dn bog-food the platofrm), but they generally stay out of the way of users

Which means that users get to dynamically manage their own data pipelines, while the pipelines team manages the operations
to ensure that the primitives and components scale to a global fleet of IoT devices. Not an easy challenge, but one that I
am certainly looking forward to.  

# References

 (0) https://kafka-summit.org/sessions/whats-inside-black-box-using-ml-tune-manage-kafka/
 (1) https://medium.com/workday-engineering/large-message-handling-with-kafka-chunking-vs-external-store-33b0fc4ccf14
 (2) https://www.slideshare.net/JiangjieQin/handle-large-messages-in-apache-kafka-58692297
 (3) https://github.com/linkedin/li-apache-kafka-clients
 (4) Kafka summit talk
 (5) https://www.confluent.io/blog/okay-store-data-apache-kafka/
 (6) https://github.com/lyft/amundsen

-----

# Things to include, if we have more space
 - schema evolution considerations, you probably want forward
 - circuadian load smearing
 - k8s integration
 - pinned groups to limit bucket writes
	 - semi-dynamic partitioning such that consumer's figure out bucket to partition mapping and then use that to claim buckets in race
