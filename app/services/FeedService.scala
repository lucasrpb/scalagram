package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.sksamuel.pulsar4s.akka.streams.sink
import com.sksamuel.pulsar4s.{ProducerConfig, ProducerMessage, Topic}
import config.PulsarConfig
import connections.PulsarConnection
import org.apache.pulsar.client.api.{MessageId, PulsarClient, Schema}
import play.api.{Configuration, Logging}
import play.api.inject.ApplicationLifecycle

import javax.inject.{Inject, Singleton}
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedService @Inject()(implicit val ec: ExecutionContext,
                            val lifecycle: ApplicationLifecycle,
                            val playConfig: Configuration,
                            val pulsarConnection: PulsarConnection
                           ) extends Logging {

  import pulsarConnection._

  logger.info(s"${Console.MAGENTA_B}FEED SERVICE INITIATED...${Console.RESET}")

  val PULSAR_SERVICE_URL = pulsarConfig.serviceURL
  val TOPIC = pulsarConfig.jobsTopic

  implicit val system = ActorSystem.create[Nothing](Behaviors.empty[Nothing], "feed-service")
  implicit val mat = Materializer(system)

  implicit val schema: Schema[Array[Byte]] = Schema.BYTES

  protected val producer = () => client.producer[Array[Byte]](ProducerConfig(topic = Topic(TOPIC),
    enableBatching = Some(false), blockIfQueueFull = Some(false)))

  def send(data: Array[Byte]): Unit = {
    val record = ProducerMessage[Array[Byte]](data)
    Source.single(record).to(sink(producer)).run()
  }

}
