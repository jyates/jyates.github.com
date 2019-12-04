# Confluent Post: IoT Challenges and Techniques

An overiew of some of the common IoT processing challenges and then some techniques that you can apply to your problem.

Problem Statement
 - large number of devices
 - (some) low latency needs
 - NO data loss - every message counts
 - thundering herds
 - large messages
 - custom formats
 
Before even deciding on technologies, the first question we should ask is, "what kind of properties do I need from my 
system", given the problem at hand. There are a number of things, but it seems like the key properties are:
 - durable storage
 - easy horizontal scalability
 - high throughput AND low-latency

The obvious choice here is Kafka - on top of fulfulling all of our requirements, it also is very stable, well supported
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
 the client so they avoid having to re-send the data.
 To not 
overwhelm any 
single partition, while continuing to be able
 to easily horizontally 
scale the easiest thing to do is to have a relatively random partitioning. The simple choice of key here is the epoch
 millisecond that the message was _received_, giving you a reasonably equal distribution of data over even a few 
 seconds of events. The advantage here is that we are helping to ensure that (a) we land data as quickly as possible 
 while (b) helping ease operational burden and growth. The obvious disadvantage is that if a device is sending us 
 multiple messages, it might make sense to handle them on the same consumer, depending on what state we need. For 
 now, we will assume the latter is not a problem for the parsing stage. However, you could potentially borrow some of
  the partitioning ideas we will discuss later here if that is a challenge in your environment.

Now let's look into the 'big message' problem. Anything over 20MB is going to start getting really challenging for 
Kafka, especially in a shared cluster that also needs things like low latency. There are two main approaches we can 
take here: chunking up the message into pieces and storing a 'message reference' in Kafka to an external store. There
 are a number of posts written about each approach (1) (2) and even some projects to help us handle large messages 
 (3). However, let's make life easy by taking the reference approach and assuming the messages are stored in S3. S3 
 is particularly nice - beyond just reliability and being the defacto blob API,in case we want to move our 
 implementation - as we can set a TTL on the bucket, so it will automatically cleanup messages that don't get 
 delivered in failure cases. So now we have messages that come into our Kafka with a small wrapper, that includes the
  data as well as some helpful things about the message:

  ```
  Message {
    string device_id;
    long arrival_time_millis;
    optional string reference;
    optional bytes body;
  }
  ```
where our event key (used for partitioning) is again, just the `arrival_time_millis`.
 
 
not 
put the pressure on Kafka
 and offload storage of the events to a remote store (you could also take a "chunking" approach to the   (1) (2).
 
 (1) https://medium.com/workday-engineering/large-message-handling-with-kafka-chunking-vs-external-store-33b0fc4ccf14
 (2) https://www.slideshare.net/JiangjieQin/handle-large-messages-in-apache-kafka-58692297
 (3) https://github.com/linkedin/li-apache-kafka-clients
Handling Scale and Herds
 - random partitioning key
   -  ensure that data lands
 - partitioning canonical to group by
   - time bucket, UUID bucket

Large Messages
 - message by reference
 - truncation
 - fast lane, slow lane, fat lane
 - intra-partition/record parallelism

Formats and schema
 - provide simple interface to parsing
 - limit complexity exposed to users
 - avoid proliferation, focus on a couple of standard formats
   - if you are producing server-side, rarely a reason to not have schemaful messages

Looking forward
 - providing ownership to data owners to their own data flows
 - democratizing operational excellence
 - flow lineage and management


Things to include, if we have more space
-------------
 - schema evolution considerations, you probably want forward
 - circuadian load smearing
 - k8s integration
 - pinned groups to limit bucket writes
	 - semi-dynamic partitioning such that consumer's figure out bucket to partition mapping and then use that to claim buckets in race
