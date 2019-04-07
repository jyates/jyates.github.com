---
layout: post
title: Just Right Parallelism in Akka Streams
location: San Francisco, CA
tags: kafka streams, etl, iot, akka, parallel, big data
---

Reliability scaling and managing streaming ingest - particularly when dealing IoT - is a challenging problem. Not only do you have to be low latency, correct and high volumes, you also get huge messages and bursty devices. On top of that, firmware developers have their own goals and are not optimizing for ease of ingest, so you have to deal with many many different data formats. What is an engineer to do?

I've come to find the combination of [Akka Streams] and the [akka-streams-kafka] library a powerful combination that solves many of my problems, while giving you release valves to easily do custom things when you need to. You probably haven't heard of [Akka Streams] - its a streaming framework built on top of the rock solid Akka actor framework. That also means it is stable, reliable and battle proven. It also has some commericial support too, if you are into that kind of thing.

[Akka Streams] is built following the [Reactive Manifesto] - it is designed with non-blocking back-pressuring so your apps run lightnining fast. You are really only limited by your slowest step (allowing you to approach the limits of [Amdahl's Law]). The API is similar to many common ETL frameworks; you stream a set of messages and have primitives to filter, groupBy, reduce, foldLeft, batch, etc., as well as develop your own custom processing stages.

If you are interesting in the some of basics of using Akka Streams, I'd suggest checking out my friend [Colin Breck's blog] where he looks at some of the core components, how you can quickly compose them together and then how you can easily add in parallelism.

We are going to pick up from Colin's posts and look at how you can take that easy parallelism and shoot yourself in the foot. :)

First, let's setup a simple flow from a Kafka topic, through some custom logic (which could include sending to another topic, writing to some database, or anything else you could want), and then commits out progress back to Kafka.

```scala
object App {

  def main(args: Array[String]): Unit = {
    // setup the consumer to read from Kafka
    val conf = ConfigFactory.load()
    val appConf = conf.getConfig("my-app")
    val topic = appConf.getString("source-topic")
    val destTopic = appConf.getString("dest-topic")
    val control = Consumer.committableSource(consumerSettings(conf), Subscriptions.topics(topic))
      .via(downstream(appConf))
      # batch commits so we flush either every 1000 records or 1 minute
      .toMat(Committer.sink(new CommitterSettings(1000, 1.minute, 1)))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()
  }

  def consumerSettings(conf: Config): ConsumerSettings[String, Array[Byte]] ={
    ConsumerSettings.create(conf.getConfig("akka.kafka.consumer"),
      new StringDeserializer(), new ByteArrayDeserializer())
      .withBootstrapServers("localhost:9092")
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  }

  def downstream(conf: Config): Flow[CommittableMessage[String, Array[Byte]], CommittableOffset, Any] = ...
}
```

Note that this flow has at-least once guarantees. We could fail after doing the destination step, but before committing. Thus, our downstream needs to be able to handle the potential repeats (yes, it is definitely 100% going to happen, especially at scale).

The interesting work is in that pesky `downstream` method.

## Parsing a record and sending it downstream

In the IoT space, its very common to not get one record per message, but rather a bunch of messsages - its generally much more efficient to send over the wire, saves space with compression, etc. Even if you have very well formatted, easy to work with devices sending you JSON then you (a) live a charmed life, and (b) are still gonna need to unpack that message.

Let's assume we want to parse messages with configurable parallelism (gotta use those cores!) and each mesage will parse into an iterator, making our lives simpler when we want to support parsing other data types.

Keeping this in akka-streams, there a bunch of primitives that can make this a rather straightforwared translation.

```scala
 type Message = Tuple2[Map[String, Object], CommittableMessage[String, Array[Byte]]]

 def downstream(conf: Config):
  Flow[CommittableMessage[String, Array[Byte]], CommittableOffset, Any] = {
	Flow[CommittableMessage[String, Array[Byte]]].mapAsync(conf.getInt("parser-parallelism")) { msg =>
      // generate the iterator from the record
      Future((msg, parse(msg.record.value())))
    }.map(tuple => {
     // make sure the last message in the iterator is the committable one
     // we don't want to commit before its fully processed!
      val iter = tuple._2.map { m => (null, m) }
      val end = Iterator.single((tuple._1, null))
      (iter ++ end).asInstanceOf[Iterator[Message]]
    })
    // flatten that iterator back out to the stream
    .mapConcat[Message](toIterable)
    // send to our downstream destination, e.g. the database
    .map(event => {
      if (event._1 != null) {
        sendDownstream(event._1)
      }
      event
    })
    // just grab back out our original, committable event
    .filter(event => event._2 != null)
    .map(_._2)
    // just pass the offset to commit back, which is handled by caller
    .map(_.committableOffset)
  }

  def toIterable[A](elements: Iterator[A]): Iterable[A] = new Iterable[A] {
    override def iterator: Iterator[A] = elements
  }
```

_You can [find the full code for this example here](https://github.com/jyates/jyates.github.com/blob/master/_code/akka-streams-kafka/src/main/scala/blog/just_right_parallelism/App0.scala)_

And that could take you pretty far - maybe indefinitely - if your stream isn't too high volume or you just handle small JSON blobs.

So where does this fall over?

The key understanding is in that **the mapAsync only applies over the creation of the iterator**. With parsing smaller JSON you can materialize that stream entirely in memory at once and get great parallelism because we are just sending materialized elements downstream.

That `mapConcat` does **not** execute in parallel - each iterator is going to be extracted in series, so we are going to be fundamentally limited in our throughput.

## Handling big messages

For more complex parsers or big blobs, you will want to produce each event in a streaming fashion. We can take almost the same model as above, but actually do all that work _inside the mapAsync with another Stream instance_. This gets us the parallelism we thought we were getting.

As a bonus, we also get to _process_ the messages out of order, while leveraging `mapAsync` to ensure that we continue to _commit in order_ (`mapAsync` ensures ordering of results). That means the impact of p90+ size messages - those unusually large ones - is dramatically reduced.

That is, a random big message does not block the whole stream from making progress. We will still not commit any of the downstream message until the big message is processed, but then they will all commit at once.

Now our downstream handling can actually be quite succinct _and_ lightning fast.

```scala
  def downstream(conf: Config):
  Flow[CommittableMessage[String, Array[Byte]], CommittableOffset, Any] = {
    Flow[CommittableMessage[String, Array[Byte]]].mapAsync(conf.getInt("parser-parallelism")) { msg =>
      Source.fromIterator(() => parse(msg.record.value()))
        .via(sendDownstream)
        .runFold(msg.committableOffset)((offset, _) => offset)
    }
  }

  def sendDownstream: Flow[Map[String, Object], Any, Any] = { ... }
```
_You can [find the full code here](https://github.com/jyates/jyates.github.com/blob/master/_code/akka-streams-kafka/src/main/scala/blog/just_right_parallelism/App.scala)_

Here we are changing our `sendDownstream` definition to a `Flow` - actually a much simpler to read approach! Now we get the expected parallelism when parsing a records and sending it downstream, ensuring that big records don't block the flow.

Not only that, now we continue to use the Streams primitives in a composable way, ensuring that the cost of restructuring is small, testing is cohesive and that future readers are not context switching (not to be underestimated!).

Unfortunately, our implementation does hide complexity around handling exceptions - do you fail the stream if the Iterator creation throws an exception? what if the Iterator throws an exception when getting the next record? That is all left as an exercise for the reader, and is highly dependent on what guarantees you want to offer users.


# Futher Implications

Now, there is a trade-off to make above: the amount of parallelism. Because we need to keep ordering (so we don't incorrectly mark messages committed), the stream throughput is inherently limited by the slowest message to parse - assuming that you aren't already blocking somehwere else. Thus, increasing the parallelism can increase your _average_ throughput; you are trading CPU cycles for increased throughput. However, by increasing parallelism you could see switching costs actually leading to _higher average latency_ per record.

That said, when viewed outside the processor, you could actually be decreasing latency when increasing throughput as small records would block until the large record is complete and then suddenly skip forward quickly.

As an example, lets assume we are using a `mapAsync` parallelism of 4 (`my-app.parser-parallelism` in our example configuration). Then we start processing 4 records in parallel.

For illustraction, lets assume the first record is the largest. While record (1) is parsing, records (2), (3), and (4) are also being parsed and flowing downstream. Akka streams is buffering their output - the `CommittableOffset` - until record (1) is complete, ensuring that we get correct ordering. Eventually, record (1) completes, and then immediately after records (2), (3), and (4) are seen to complete. Thus, it can apprear that their processing time is **approximately zero**.

That is also why its important to have metrics intra-stream as well, so you can understand the performance of your parser/downstream logic, as well as your ingest engine. This becomes even more important when building out a streaming _platform_, where the parser is no longer under your control and you need to export an understanding of the stream performance.

## Downstream pressure

Not only do you have consider the tradeoffs in throughput, but also the effect on the downstream components. Since this is all running on the JVM you coud easily hit a GC that causes the Kakfa Consumer Group to rebalance. This means that your processor now has to rewind and reprocess the same messages over again. This can mean lots of repeat events sent downstream. In particularly bursty streams, this could easily see repeat parsers of 10+ times. So now you are wasting CPU, memory and I/O.

You need to consider if the latency requirements are necessary and that you can tolerate these occasional repeats (your milage may vary here - everyone's data is different). It could actually be better to parser just one record at a time because the restart effort is very large or you can only tolerate limited pressure on your downstream.

The tradeoff is that you are inherently limiting your throughput in favor of avoiding repeats.

Note that in this case, you are actually better off just flattening your stream into a `map` and a `mapConcat` stage. The overhead of the `mapAsync` parallelism is going to just be wasteful ([You can read more about managing parallelism here](https://doc.akka.io/docs/akka/current/stream/stream-parallelism.html)).

## Managing large messages

In "big data" there is inherently the implication that the long-tail is just part of life. These 'big messages' that mess with your throughput (and potentially cause lots of repeats) will be normal.

After you quantify the quantity and effect of these messages, you then have to decide what to do with them. While you could adjust down parallelism, as we talked about above, maybe your latency requirements or parsing profile mean that is untenable.

An option is to run two different consumer groups. One that handles the small messages that parse and play together "nicely", and a second that mess everything up. This means you can then build two very different tuning profiles to deal with each group independently. Also, these big messages are no longer blocking your small messages and you can then also likely dramatically reduce your average latency for small _and large_ messages dramatically.

# Wrap up

[Akka Streams] combined with the [akka-streams-kafka] library provides an incredibly powerful set of primatives that can be combined to provide a lightning fast streaming ingest platform. As with any powerful tool, there are sharp edges that you can cut yourseful on. However, you can get surprisingly good performance out of the box - a testament to [akka-streams]. If you are looking to wring performance or have an unique use case, you need to have a deeper understanding. Here we have seen how we can compose some basic primitives together to not only wring extra perforamnce out of our stream, but also handle some of the unique properties of IoT messaging handing.

Rather than wiping out, we can tame that long tail and surf the wave of big data.


[Akka Streams]: https://doc.akka.io/docs/akka/2.5/stream/
[akka-streams-kafka]: https://doc.akka.io/docs/akka-stream-kafka/current/home.html
[Lightbend]: https://www.lightbend.com/
[Reactive Manifesto]: https://www.reactivemanifesto.org/
[Amdahl's Law]: https://en.wikipedia.org/wiki/Amdahl%27s_law
[Colin Breck's blog]: https://blog.colinbreck.com/
