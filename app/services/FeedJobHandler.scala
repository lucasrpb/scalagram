package services

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import app.Constants
import com.sksamuel.pulsar4s.{ConsumerConfig, ConsumerMessage, ProducerConfig, ProducerMessage, PulsarClient, PulsarClientConfig, Subscription, Topic}
import com.sksamuel.pulsar4s.akka.streams.{sink, source}
import config.PulsarConfig
import connections.PulsarConnection
import models.{Feed, FeedJob}
import org.apache.pulsar.client.api.{Authentication, AuthenticationFactory, MessageId, Schema, SubscriptionInitialPosition, SubscriptionType}
import play.api.{Configuration, Logging}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import repositories.FeedRepository

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedJobHandler @Inject()(implicit val ec: ExecutionContext,
                               lifecycle: ApplicationLifecycle,
                               val feedRepo: FeedRepository,
                               val pulsarConnection: PulsarConnection
                              ) extends Logging {

  import pulsarConnection._

  val TOPIC = pulsarConfig.jobsTopic

  logger.info(s"${Console.MAGENTA_B}FEED PROCESSOR INITIATED...${Console.RESET}")

  protected val producer = () => client.producer[Array[Byte]](ProducerConfig(topic = Topic(TOPIC),
    enableBatching = Some(false), blockIfQueueFull = Some(false)))

  /*lifecycle.addStopHook { () =>
    for {
      _ <- client.closeAsync
    } yield {}
  }*/

  def send(data: Array[Byte]): Unit = {
    val record = ProducerMessage[Array[Byte]](data)
    Source.single(record).to(sink(producer)).run()
  }

  val consumerFn = () => client.consumer(ConsumerConfig(subscriptionName = Subscription(s"feed-job-handler-${UUID.randomUUID.toString}"),
    topics = Seq(Topic(TOPIC)),
    subscriptionType = Some(SubscriptionType.Exclusive),
    subscriptionInitialPosition = Some(SubscriptionInitialPosition.Latest)),
  )

  val consumerSource = source(consumerFn, Some(MessageId.latest))

  def handler(msg: ConsumerMessage[Array[Byte]]): Future[Boolean] = {
    val job = Json.parse(msg.value).as[FeedJob]

    logger.info(s"${Console.GREEN_B}PROCESSING JOB: ${job.followers}${Console.RESET}\n")

    feedRepo.insertPostIds(job.followers.map { f =>
      Feed(
        job.postId,
        f,
        job.fromUserId,
        job.postedAt
      )
    }).flatMap { ok =>

      feedRepo.getFollowerIds(job.fromUserId, job.lastId, Constants.MAX_FOLLOWERS_POLL).map { followers =>

        logger.info(s"${Console.BLUE_B}more followers: ${followers}${Console.RESET}\n")

        if(!followers.isEmpty){
          send(Json.toBytes(Json.toJson(
            FeedJob(
              job.postId,
              job.fromUserId,
              job.postedAt,
              followers,
              followers.lastOption
            )
          )))

          true

        } else {
          false
        }

      }

    }.recover {case _ => true}

  }

  consumerSource.
    mapAsync(1)(handler)
    .run()

}