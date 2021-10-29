package services

import config.PulsarConfig
import org.apache.pulsar.client.api.{MessageId, PulsarClient}
import play.api.{Configuration, Logging}
import play.api.inject.ApplicationLifecycle

import javax.inject.{Inject, Singleton}
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedService @Inject()(implicit val ec: ExecutionContext,
                            val lifecycle: ApplicationLifecycle,
                            val playConfig: Configuration
                           ) extends Logging {

  val pulsarConfig: PulsarConfig = playConfig.get[PulsarConfig]("pulsar")

  logger.info(s"${Console.MAGENTA_B}FEED SERVICE INITIATED...${Console.RESET}")

  val PULSAR_SERVICE_URL = pulsarConfig.serviceURL
  val TOPIC = pulsarConfig.jobsTopic

  protected val client = PulsarClient.builder()
    .serviceUrl(PULSAR_SERVICE_URL)
    .build()

  protected val producer = client.newProducer()
    .topic(TOPIC)
    .create()

  lifecycle.addStopHook { () =>
    Future.successful(producer.closeAsync().toCompletableFuture.toScala.flatMap(_ => client.closeAsync().toCompletableFuture.toScala))
  }

  def send(data: Array[Byte]): Future[MessageId] = {
    producer.sendAsync(data).toCompletableFuture.toScala
  }

}
