package controllers

import actions.LoginAction
import app.Cache
import models.{Profile, SessionInfo, UserUpdate}
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import play.api.mvc._
import repositories.ProfileRepository

import java.util.UUID
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProfileController @Inject()(val controllerComponents: ControllerComponents,
                                  val repo: ProfileRepository,
                                  val loginAction: LoginAction,
                                  val cache: Cache,
                                  implicit val ec: ExecutionContext
                                 ) extends BaseController {

  def upsert() = loginAction.async { implicit request: Request[AnyContent] =>

    cache.get(request.session.data.get("sessionId").getOrElse("")).flatMap {
      case None => Future.successful(InternalServerError(Json.obj(
        "error" -> JsString("Session not found!")
      )).withNewSession)

      case Some(bytes) =>

        val session = Json.parse(bytes).as[SessionInfo]

        val data = (request.body.asJson.get.as[JsObject] ++ Json.obj("userId" -> JsString(session.id))).as[Profile]
        val id = UUID.fromString(session.id)

        repo.upsert(id, data).map(ok => Ok(Json.obj(
          "ok" -> JsBoolean(ok)
        )))

    }
  }

  def getProfile(id: String) = Action.async { implicit request: Request[AnyContent] =>
    repo.get(UUID.fromString(id)).map {
      case None => NotFound(Json.obj(
        "error" -> JsString("Profile not found")
      ))
      case Some(profile) => Ok(Json.toJson(profile))
    }
  }

}
