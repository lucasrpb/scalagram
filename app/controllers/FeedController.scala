package controllers

import actions.LoginAction
import app.Cache
import models.{Follower, SessionInfo}
import play.api.libs.json.{JsObject, JsString, Json, Reads}
import play.api.mvc._
import repositories.FeedRepository
import Follower._
import play.api.Logging
import services.FeedJobHandler

import java.util.UUID
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import akka.stream.scaladsl._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class FeedController @Inject()(val controllerComponents: ControllerComponents,
                               val repo: FeedRepository,
                               val loginAction: LoginAction,
                               val feedJobHandler: FeedJobHandler,
                               val cache: Cache,
                               implicit val ec: ExecutionContext) extends BaseController with Logging {

  def follow() = loginAction.async { implicit request: Request[AnyContent] =>

    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
    val data = (request.body.asJson.get.as[JsObject] ++ Json.obj("userId" -> JsString(session.id))).as[Follower]
    val id = UUID.fromString(session.id)

    repo.follow(id, data).map {
      case false => Ok(Json.obj(
        "error" -> JsString("You already follow this user!")
      ))
      case _ => Ok(Json.obj(
        "status" -> JsString("You successfully followed this user!")
      ))
    }
  }

  def unfollow(followerId: String) = loginAction.async { implicit request: Request[AnyContent] =>
    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
    val id = UUID.fromString(session.id)

    repo.unfollow(id, UUID.fromString(followerId)).map {
      case false => InternalServerError(Json.obj(
        "error" -> JsString("Something bad happened!")
      ))
      case _ => Ok(Json.obj(
        "status" -> JsString("You successfully unfollowed this user!")
      ))
    }
  }

  def getFollowers(start: Int, n: Int) = loginAction.async { implicit request: Request[AnyContent] =>
    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
    val id = UUID.fromString(session.id)

    repo.getFollowers(id, start, n).map(followers => Ok(Json.toJson(followers)))
  }

  def getFeed(start: Int, n: Int) = loginAction.async { implicit request: Request[AnyContent] =>
    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
    val id = UUID.fromString(session.id)

    val tags = request.body.asJson.map(_.as[List[String]]).getOrElse(List.empty[String]).map(_.toLowerCase.trim)

    repo.getFeedPosts(id, start, n, tags).map(posts => Ok(Json.toJson(posts)))
  }

  def feedStream(id: String) = WebSocket.accept[String, String] { request =>
    // Just ignore the input
    val in = Sink.ignore

    // Send a single 'Hello!' message and close
    val out = Source.single(s"Hello, ${id}!")

    Flow.fromSinkAndSource(in, out)
  }

}
