package controllers

import actions.LoginAction
import app.Cache
import models.{Follower, SessionInfo}
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc._
import repositories.FeedRepository

import java.util.UUID
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class FeedController @Inject()(val controllerComponents: ControllerComponents,
                               val repo: FeedRepository,
                               val loginAction: LoginAction,
                               val cache: Cache,
                               implicit val ec: ExecutionContext) extends BaseController {

  def follow() = loginAction.async { implicit request: Request[AnyContent] =>

    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
    val data = (request.body.asJson.get.as[JsObject] ++ Json.obj("userId" -> JsString(session.id))).as[Follower]
    val id = UUID.fromString(session.id)

    repo.follow(id, data).map {
      case false => Ok(Json.obj(
        "error" -> JsString("You already follow this user!")
      ))
      case _ => Ok(Json.obj(
        "status" -> JsString("You succesfully followed this user!")
      ))
    }
  }

}
