package controllers

import actions.LoginAction
import app.Cache
import models.{Post, Profile, SessionInfo}
import play.api.Logging
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc._
import repositories.PostRepository

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
                               val repo: PostRepository,
                               val loginAction: LoginAction,
                               val cache: Cache,
                               implicit val ec: ExecutionContext) extends BaseController with Logging {

  def insert() = loginAction.async { implicit request: Request[AnyContent] =>
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
  }

  // Use binary in postman if using body parser parse.temporaryFile...

  def upload() = loginAction.async(parse.multipartFormData) { implicit request =>

    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
    val id = UUID.fromString(session.id)

    val imgOpt = request.body.file("img")
    val dataOpt = request.body.dataParts.get("data")

    if(imgOpt.isEmpty){
      Future.successful(NotFound(Json.obj(
        "error" -> JsString("Missing image!")
      )))
    } else if(dataOpt.isEmpty) {
      Future.successful(NotFound(Json.obj(
        "error" -> JsString("Missing data!")
      )))
    } else {
      val file = imgOpt.get
      val img = file.ref
      val fileId = UUID.randomUUID()
      val data = (Json.parse(dataOpt.get.head.getBytes()).as[JsObject] ++ Json.obj(
        "userId" -> JsString(session.id),
        "id" -> JsString(fileId.toString)
      )).as[Post]

      val path = img.moveTo(Paths.get(s"posts/${fileId.toString}.${com.google.common.io.Files.getFileExtension(file.filename)}"), replace = true)

      if(Files.exists(path)){

        repo.insert(id, data).map {
          case false => InternalServerError(Json.obj(
            "error" -> JsString("Some error occurred!")
          ))

          case true => Ok(Json.obj(
            "status" -> JsString("Post inserted successfully!")
          ))
        }

      } else {
        Future.successful(InternalServerError(Json.obj(
          "error" -> "There was an error uploading the file!"
        )))
      }
    }
  }

}
