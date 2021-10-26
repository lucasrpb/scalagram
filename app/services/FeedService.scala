package services

import org.apache.pulsar.client.api.{MessageId, PulsarClient}
import play.api.Logging
import play.api.inject.ApplicationLifecycle

import javax.inject.{Inject, Singleton}
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedService @Inject()(implicit val ec: ExecutionContext, lifecycle: ApplicationLifecycle) extends Logging {

  logger.info(s"${Console.MAGENTA_B}FEED SERVICE INITIATED...${Console.RESET}")

  val PULSAR_SERVICE_URL = "pulsar://localhost:6650"
  val TOPIC = "public/scalagram/feed-jobs"

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
