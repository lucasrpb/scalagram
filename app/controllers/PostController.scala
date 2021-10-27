package controllers

import actions.LoginAction
import app.{Cache, Constants}
import models.{Feed, FeedJob, Post, SessionInfo}
import models.Post._
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import play.api.mvc._
import repositories.{FeedRepository, PostRepository}
import services.FeedService

import java.nio.file.{Files, Paths}
import java.util.UUID
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

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
                               implicit val ec: ExecutionContext) extends BaseController with Logging {

  // Use binary in postman if using body parser parse.temporaryFile...
  def processUpload(request: Request[MultipartFormData[TemporaryFile]]): Future[Result] = {
    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
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

    val path = img.moveTo(Paths.get(s"${Constants.IMG_UPLOAD_FOLDER}/${postId.toString}.${ext}"), replace = true)

    logger.info(s"\nextension: ${ext}\n")

    if(Files.exists(path)){
      return postRepo.insert(id, data).flatMap {
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
          ))).map { m =>
            Ok(Json.obj(
              "status" -> JsString("Post inserted successfully!")
            ))
          }

        }
      }
    }

    Future.successful(InternalServerError(Json.obj(
      "error" -> "There was an error uploading the file!"
    )))
  }

  def upload() = loginAction.async(parse.multipartFormData) { implicit request =>
    processUpload(request)
  }

  def getPostsByUserId(id: String, start: Int, n: Int) = Action.async { implicit request: Request[AnyContent] =>
    val tags = request.body.asJson.map(_.as[List[String]]).getOrElse(List.empty[String])
    postRepo.getPostsByUserId(UUID.fromString(id), start, n, tags).map(posts => Ok(Json.toJson(posts)))
  }

}
