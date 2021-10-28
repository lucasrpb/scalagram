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

  protected def isValidSession[A](request: Request[A]): Boolean = {
    if(request.session.isEmpty || request.session.data.isEmpty) return false

    val sessionId = request.session.data.get("sessionId")

    if(sessionId.isEmpty) return false

    val opt = cache.get(sessionId.get)

    if(opt.isEmpty) return false

    val info = Json.parse(opt.get).as[SessionInfo]

    val userIdSessionId = cache.get(info.id)

    if(userIdSessionId.isEmpty || new String(userIdSessionId.get).compareTo(sessionId.get) != 0) return false

    val now = System.currentTimeMillis()

    info.expiresAt > now
  }

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    if(!isValidSession(request)){
      Future.successful(Unauthorized(Json.obj(
        "error" -> JsString("You must be logged to execute this action!")
      )))
    } else {
      block(request)
    }
  }
}
