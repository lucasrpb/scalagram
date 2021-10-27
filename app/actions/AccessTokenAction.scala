package actions

import app.Cache
import models.SessionInfo
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results.{Forbidden, Unauthorized}
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class AccessTokenAction @Inject()(parser: BodyParsers.Default, val cache: Cache)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) with Logging {

  protected def isValidToken[A](request: Request[A]): Boolean = {
    val token = request.headers.get("Authorization").map(_.replace("Bearer ", ""))

    if(token.isEmpty) return false

    val opt = cache.get(token.get)

    if(opt.isEmpty) return false

    val id = cache.get(new String(opt.get))

    if(id.isEmpty) return false

    val user = cache.get(new String(id.get))

    logger.info(s"user: ${user}")

    if(user.isEmpty) return false

    val info = Json.parse(user.get).as[SessionInfo]

    logger.info(s"info: ${info}")

    val now = System.currentTimeMillis()

    info.token.compareTo(token.get) == 0 && now < info.expiresAt
  }

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    if(!isValidToken(request)){
      Future.successful(Unauthorized("Invalid access token!"))
    } else {
      block(request)
    }
  }
}
