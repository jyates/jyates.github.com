package blog.just_right_parallelism

import akka.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Keep}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.collection.immutable.Iterable
import scala.concurrent.Future
import scala.concurrent.duration._

object App0 {

  def main(args: Array[String]): Unit = {
    // setup the consumer to read from Kafka
    val conf = ConfigFactory.load()
    val appConf = conf.getConfig("my-app")
    val topic = appConf.getString("source-topic")
    val destTopic = appConf.getString("dest-topic")
    val control = Consumer.committableSource(consumerSettings(conf), Subscriptions.topics(topic))
      .via(downstream(appConf))
      .toMat(Committer.sink(new CommitterSettings(1000, 1.minute, 1)))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()
  }

  def consumerSettings(conf: Config): ConsumerSettings[String, Array[Byte]] = {
    ConsumerSettings.create(conf.getConfig("akka.kafka.consumer"),
      new StringDeserializer(), new ByteArrayDeserializer())
      .withBootstrapServers("localhost:9092")
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  }


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
      // just grab back out our committable event
      .filter(event => event._2 != null)
      .map(_._2)
      .map(_.committableOffset)
  }

  def toIterable[A](elements: Iterator[A]): Iterable[A] = new Iterable[A] {
    override def iterator: Iterator[A] = elements
  }

  def parse(content: Array[Byte]): Iterator[Map[String, Object]] = {
    // To Implement
    null
  }

  def sendDownstream(event: Map[String, Object]) = { // To Implement
  }
}
