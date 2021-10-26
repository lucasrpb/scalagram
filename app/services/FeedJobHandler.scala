package services

import models.{Feed, FeedJob}
import org.apache.pulsar.client.api.{Consumer, Message, MessageId, PulsarClient, SubscriptionType}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.{JsString, Json}
import repositories.FeedRepository

import javax.inject.{Inject, Singleton}
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class FeedJobHandler @Inject()(implicit val ec: ExecutionContext,
                               lifecycle: ApplicationLifecycle,
                               val feedRepo: FeedRepository
                              ) extends Logging {

  logger.info(s"${Console.MAGENTA_B}FEED PROCESSOR INITIATED...${Console.RESET}")

  val PULSAR_SERVICE_URL = "pulsar://localhost:6650"
  val TOPIC = "public/scalagram/feed-jobs"

  protected val client = PulsarClient.builder()
    .serviceUrl(PULSAR_SERVICE_URL)
    .build()

  protected val producer = client.newProducer()
    .topic(TOPIC)
    .create()

  lifecycle.addStopHook { () =>
    for {
      _ <- producer.closeAsync().toScala
      _ <- consumer.closeAsync().toScala
      _ <- client.closeAsync().toScala
    } yield {}
  }

  def send(data: Array[Byte]): Future[MessageId] = {
    producer.sendAsync(data).toCompletableFuture.toScala
  }

  protected def listener(consumer: Consumer[Array[Byte]], msg: Message[Array[Byte]]): Unit = {
    val job = Json.parse(msg.getData).as[FeedJob]

    logger.info(s"${Console.GREEN_B}PROCESSING JOB: ${job}${Console.RESET}\n")

    feedRepo.insertPostIds(job.followers.map { f =>
      Feed(
        job.fromUserId,
        f,
        job.postId,
        job.postedAt
      )
    }).onComplete {
      case Success(ok) =>

        consumer.acknowledge(msg)

        feedRepo.getFollowerIds(job.fromUserId, job.start, 2).flatMap { followers =>

          logger.info(s"${Console.BLUE_B}more followers: ${followers}${Console.RESET}\n")

          if(!followers.isEmpty){
            send(Json.toBytes(Json.toJson(
              FeedJob(
                job.postId,
                job.fromUserId,
                job.postedAt,
                followers,
                job.start + followers.length
              )
            )))
          } else {
            Future.successful(true)
          }

        }

      case Failure(ex) => consumer.negativeAcknowledge(msg)
      /*case _ => consumer.acknowledge(msg)*/
    }
  }

  protected val consumer = client.newConsumer()
    .topic(TOPIC)
    .subscriptionType(SubscriptionType.Shared)
    .subscriptionName("feed-job-handler")
    .messageListener(listener)
    .subscribe()
}