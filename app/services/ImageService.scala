package services

import akka.stream.scaladsl.Source
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.{Acl, BlobId, BlobInfo, StorageOptions}
import com.google.common.base.Charsets
import com.sksamuel.pulsar4s._
import com.sksamuel.pulsar4s.akka.streams.{sink, source}
import connections.PulsarConnection
import models.ImageJob
import org.apache.pulsar.client.api.{MessageId, SubscriptionInitialPosition, SubscriptionType}
import org.imgscalr.Scalr
import play.api.{Configuration, Logging}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import repositories.FeedRepository

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Paths}
import java.util.UUID
import javax.imageio.ImageIO
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

@Singleton
class ImageService @Inject()(implicit val ec: ExecutionContext,
                             lifecycle: ApplicationLifecycle,
                             val feedRepo: FeedRepository,
                             val config: Configuration,
                             val pulsarConnection: PulsarConnection) extends Logging {

  import pulsarConnection._

  val TOPIC = pulsarConfig.imageJobsTopic
  val BUCKET_NAME = config.get[String]("services.image.bucket")

  logger.info(s"${Console.MAGENTA_B}IMAGE SERVICE INITIATED...${Console.RESET}")

  val credentials = GoogleCredentials.fromStream(new FileInputStream(config.get[String]("google.credentials.path")))
  val storage = StorageOptions.newBuilder.setCredentials(credentials).build.getService()

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

  val consumerJobFn = () => client.consumer(ConsumerConfig(subscriptionName = Subscription(s"image-job-handler-${UUID.randomUUID.toString}"),
    topics = Seq(Topic(TOPIC)),
    subscriptionType = Some(SubscriptionType.Shared),
    subscriptionInitialPosition = Some(SubscriptionInitialPosition.Latest)),
  )

  val consumerJobSource = source(consumerJobFn, Some(MessageId.latest))

  def resize(input: File, ext: String): Unit = {
    val originalImage = ImageIO.read(input)

    var img_width = originalImage.getWidth
    var img_height = originalImage.getHeight

    val maxWidth = 400
    val maxHeight = 400

    // Scale image to fit in a square but keeping aspect ratio
    if(img_width > maxWidth || img_height > maxHeight) {
      val factx = img_width.toDouble / maxWidth
      val facty = img_height.toDouble / maxHeight
      val fact = if(factx>facty) factx else facty
      img_width = (img_width / fact).toInt
      img_height =(img_height / fact).toInt
    }

    // we want image in png format
    ImageIO.write(Scalr.resize(originalImage, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, img_width, img_height, Scalr.OP_ANTIALIAS), ext, input)
  }

  def handler(msg: ConsumerMessage[Array[Byte]]): Future[Boolean] = {
    val job = Json.parse(msg.value).as[ImageJob]

    logger.info(s"${Console.GREEN_B}PROCESSING IMAGE JOB: ${job}${Console.RESET}\n")

    val blobId = BlobId.of(BUCKET_NAME, s"${job.id.toString}.${job.ext}")
    val blobInfo = BlobInfo.newBuilder(blobId)
      .setAcl(Seq(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)).asJava)
      .setContentType("application/image").build()

    val p = Promise[Boolean]()

    try {
      val file = Paths.get(job.filePath)

      resize(file.toFile, job.ext)

      val bytes = Files.readAllBytes(file)
      val blob = storage.create(blobInfo, bytes)

      logger.info(blob.toString)

      logger.info(
        "File " + job.filePath + " uploaded to bucket " + BUCKET_NAME + " as " + job.id)

      val producer = () => client.producer[Array[Byte]](ProducerConfig(topic = Topic(job.topic),
        enableBatching = Some(false), blockIfQueueFull = Some(false)))

      val record = ProducerMessage[Array[Byte]](job.id.toString.getBytes(Charsets.UTF_8))
      Source.single(record).to(sink(producer)).run()

      file.toFile.delete()

      p.success(true)

    } catch {
      case ex: Throwable =>
        p.success(false)
        logger.error(ex.toString)
    }

    p.future
  }

  consumerJobSource.
    mapAsync(1)(handler)
    .run()

}