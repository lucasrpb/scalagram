package actions

import app.Cache
import models.SessionInfo
import play.api.Logging
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Results.Unauthorized
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LoginAction @Inject()(parser: BodyParsers.Default, val cache: Cache)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) with Logging {

  protected def isValidSession[A](request: Request[A]): Future[Boolean] = {

    if(request.session.isEmpty || request.session.data.isEmpty) return Future.successful(false)

    val sessionId = request.session.data.get("sessionId")

    if(sessionId.isEmpty) return Future.successful(false)

    cache.get(request.session.data.get("sessionId").getOrElse("")).flatMap {
      case None => Future.successful(false)
      case Some(bytes) => cache.get(sessionId.get).flatMap {
        case None => Future.successful(false)
        case Some(bytes) =>

          val info = Json.parse(bytes).as[SessionInfo]

          logger.debug(s"session info: ${info}")

          cache.get(info.id).map {
            case None => false
            case Some(bytes) =>

              val userIdSessionId = new String(bytes)

              !userIdSessionId.isEmpty && info.expiresAt > System.currentTimeMillis()
          }

      }
    }
  }

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    isValidSession(request).flatMap {
      case false => Future.successful(Unauthorized(Json.obj(
        "error" -> JsString("You must be logged to execute this action!")
      )))

      case true => block(request)
    }
  }
}
