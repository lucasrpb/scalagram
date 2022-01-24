package controllers

import actions.LoginAction
import app.{Cache, Constants}
import com.google.common.base.Charsets
import com.sksamuel.pulsar4s.akka.streams.source
import com.sksamuel.pulsar4s.{ConsumerConfig, ConsumerMessage, Subscription, Topic}
import connections.PulsarConnection
import models.{Comment, Feed, FeedJob, ImageJob, Post, SessionInfo, UpdateComment, UpdatePost}
import models.Post._
import org.apache.pulsar.client.api.{MessageId, SubscriptionInitialPosition, SubscriptionType}
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json}
import play.api.mvc._
import repositories.{FeedRepository, PostRepository}
import services.{FeedService, ImageService}

import java.nio.file.{Files, Paths}
import java.util.UUID
import javax.inject._
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class PostController @Inject()(val controllerComponents: ControllerComponents,
                               val postRepo: PostRepository,
                               val feedRepo: FeedRepository,
                               val loginAction: LoginAction,
                               val cache: Cache,
                               val feedService: FeedService,
                               val imageService: ImageService,
                               val pulsarConnection: PulsarConnection,
                               implicit val ec: ExecutionContext) extends BaseController with Logging {

  import pulsarConnection._

  val clientId = UUID.randomUUID.toString
  val TOPIC = s"non-persistent://scalagram-app/scalagram/img-api-client"

  val imgConsumerFn = () => client.consumer(ConsumerConfig(subscriptionName = Subscription(s"image-job-handler-${clientId}"),
    topics = Seq(Topic(TOPIC)),
    subscriptionType = Some(SubscriptionType.Exclusive),
    subscriptionInitialPosition = Some(SubscriptionInitialPosition.Latest)),
  )

  val imgConsumer = source(imgConsumerFn, Some(MessageId.latest))

  val promises = TrieMap.empty[String, Promise[Boolean]]

  def handler(msg: ConsumerMessage[Array[Byte]]): Future[Boolean] = {

    logger.debug(s"${Console.CYAN_B}IMG PROCESSING ENDED: ${msg}${Console.RESET}")

    promises.remove(new String(msg.value, Charsets.UTF_8)).map(_.success(true))

    Future.successful(true)
  }

  imgConsumer.
    mapAsync(1)(handler)
    .run()

  def processUpload(request: Request[MultipartFormData[TemporaryFile]], bytes: Array[Byte]): Future[Result] = {

    val session = Json.parse(bytes).as[SessionInfo]
    val id = UUID.fromString(session.id)

    val imgOpt = request.body.file("img")
    val dataOpt = request.body.dataParts.get("data")

    if(imgOpt.isEmpty){
      return Future.successful(NotFound(Json.obj(
        "error" -> JsString("Missing image!")
      )))
    }

    if(dataOpt.isEmpty) {
      return Future.successful(NotFound(Json.obj(
        "error" -> JsString("Missing data!")
      )))
    }

    val file = imgOpt.get
    val img = file.ref
    val postId = UUID.randomUUID()
    val ext = com.google.common.io.Files.getFileExtension(file.filename)

    val data = (Json.parse(dataOpt.get.head.getBytes()).as[JsObject] ++ Json.obj(
      "userId" -> JsString(session.id),
      "imgType" -> JsString(ext),
      "id" -> JsString(postId.toString)
    )).as[Post]

    //val path = img.moveTo(Paths.get(s"${Constants.IMG_UPLOAD_FOLDER}/${postId.toString}.${ext}"), replace = true)

    logger.info(s"\nextension: ${ext}\n")

    val ps = Promise[Boolean]()

    promises += data.id.toString -> ps

    if(Files.exists(img)){

      val job = ImageJob(data.id, ext, img.path.toString, TOPIC)

      imageService.send(Json.toBytes(Json.toJson(job)))

      return ps.future.flatMap(_ => postRepo.insert(data)).flatMap {
        case false => Future.successful(InternalServerError(Json.obj(
          "error" -> JsString("Some error occurred!")
        )))

        case true => feedRepo.getFollowerIds(id, None, Constants.MAX_FOLLOWERS_POLL).flatMap { followers =>

          // User should be able to see its own posts...
          val all = (followers :+ data.userId).sortBy(_.toString)

          feedService.send(Json.toBytes(Json.toJson(
            FeedJob(
              data.id,
              data.userId,
              data.postedAt,
              all,
              all.lastOption
            )
          )))

          //img.delete()

          Future.successful(Ok(Json.obj(
            "status" -> JsString("Post inserted successfully!")
          )))

        }
      }
    }

    Future.successful(InternalServerError(Json.obj(
      "error" -> "There was an error uploading the file!"
    )))
  }

  // Use binary in postman if using body parser parse.temporaryFile...
  def processUpload(request: Request[MultipartFormData[TemporaryFile]]): Future[Result] = {
    cache.get(request.session.data.get("sessionId").getOrElse("")).flatMap {
      case None => Future.successful(InternalServerError(Json.obj(
        "error" -> JsString("Session not found!")
      )).withNewSession)
      case Some(bytes) => processUpload(request, bytes)
    }
  }

  def upload() = loginAction.async(parse.multipartFormData) { implicit request =>
    processUpload(request)
  }

  def getPostsByUserId(id: String, start: Int, n: Int) = Action.async { implicit request: Request[AnyContent] =>
    val tags = request.body.asJson.map(_.as[List[String]]).getOrElse(List.empty[String])
    postRepo.getPostsByUserId(UUID.fromString(id), start, n, tags).map(posts => Ok(Json.toJson(posts)))
  }

  def updatePost() = loginAction.async { implicit request: Request[AnyContent] =>

    cache.get(request.session.data.get("sessionId").getOrElse("")).flatMap {
      case None => Future.successful(InternalServerError(Json.obj(
        "error" -> JsString("Session not found!")
      )).withNewSession)
      case Some(bytes) =>

        val session = Json.parse(bytes).as[SessionInfo]

        val id = UUID.fromString(session.id)

        val up = request.body.asJson.get.as[UpdatePost]

        postRepo.updatePost(id, up).map(ok => Ok(Json.toJson(ok)))

    }
  }

  def comment(postId: String) = loginAction.async { implicit request: Request[AnyContent] =>

    cache.get(request.session.data.get("sessionId").getOrElse("")).flatMap {
      case None => Future.successful(InternalServerError(Json.obj(
        "error" -> JsString("Session not found!")
      )).withNewSession)
      case Some(bytes) =>

        val session = Json.parse(bytes).as[SessionInfo]
        val id = UUID.fromString(session.id)

        val body = (request.body.asJson.get \ "body").as[JsString].value

        postRepo.insertComment(Comment(
          UUID.randomUUID,
          UUID.fromString(postId),
          id,
          body
        )).map(ok => Ok(Json.toJson(ok)))

    }
  }

  def updateComment() = loginAction.async { implicit request: Request[AnyContent] =>

    cache.get(request.session.data.get("sessionId").getOrElse("")).flatMap {
      case None => Future.successful(InternalServerError(Json.obj(
        "error" -> JsString("Session not found!")
      )).withNewSession)
      case Some(bytes) =>

        val session = Json.parse(bytes).as[SessionInfo]

        val id = UUID.fromString(session.id)

        val uc = request.body.asJson.get.as[UpdateComment]
        postRepo.updateComment(id, uc).map(ok => Ok(Json.toJson(ok)))

    }
  }

  def getComments(postId: String, start: Int, n: Int) = Action.async { implicit request: Request[AnyContent] =>
    postRepo.getComments(UUID.fromString(postId), start, n).map(posts => Ok(Json.toJson(posts)))
  }

}
