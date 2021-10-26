package controllers

import actions.LoginAction
import app.{Cache, Constants}
import models.{Feed, FeedJob, Post, SessionInfo}
import models.Post._
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, JsString, Json}
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

  /*def insert() = loginAction.async { implicit request: Request[AnyContent] =>
    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
    val data = (request.body.asJson.get.as[JsObject] ++ Json.obj("userId" -> JsString(session.id))).as[Post]
    val id = UUID.fromString(session.id)

    logger.info(s"\npost: ${data}\n")

    repo.insert(id, data).map {
      case false => InternalServerError(Json.obj(
        "error" -> JsString("Some error occurred!")
      ))

      case true => Ok(Json.obj(
        "status" -> JsString("Post inserted successfully!")
      ))
    }
  }*/

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
    val fileId = UUID.randomUUID()
    val data = (Json.parse(dataOpt.get.head.getBytes()).as[JsObject] ++ Json.obj(
      "userId" -> JsString(session.id),
      "id" -> JsString(fileId.toString)
    )).as[Post]

    val path = img.moveTo(Paths.get(s"${Constants.IMG_UPLOAD_FOLDER}/${fileId.toString}.${com.google.common.io.Files.getFileExtension(file.filename)}"), replace = true)

    if(Files.exists(path)){
      return postRepo.insert(id, data).flatMap {
        case false => Future.successful(InternalServerError(Json.obj(
          "error" -> JsString("Some error occurred!")
        )))

        case true => feedRepo.getFollowerIds(id, 0, 2).flatMap { followers =>

          if(followers.isEmpty){

            Future.successful(Ok(Json.obj(
              "status" -> JsString("Post inserted successfully!")
            )))

          } else {

            feedService.send(Json.toBytes(Json.toJson(
              FeedJob(
                data.id,
                data.userId,
                data.postedAt,
                followers :+ data.userId
              )
            ))).map { m =>
              Ok(Json.obj(
                "status" -> JsString("Post inserted successfully 2!")
              ))
            }

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
    postRepo.getPostsByUserId(UUID.fromString(id), start, n).map(posts => Ok(Json.arr(posts)))
  }

}
