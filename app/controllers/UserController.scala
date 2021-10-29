package controllers

import actions.{AccessTokenAction, LoginAction}
import app.{Cache, Constants}
import models.{CodeInfo, SessionInfo, TokenInfo, User, UserStatus, UserUpdate}
import play.api.Logging
import play.api.libs.json.{JsBoolean, JsString, Json}
import play.api.mvc._
import repositories.UserRepository

import java.util.UUID
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class UserController @Inject()(val controllerComponents: ControllerComponents,
                               val repo: UserRepository,
                               val cache: Cache,
                               val tokenAction: AccessTokenAction,
                               val loginAction: LoginAction,
                               implicit val ec: ExecutionContext) extends BaseController with Logging {

  def insert() = Action.async { implicit request: Request[AnyContent] =>
    val user = request.body.asJson.get.as[User]

    logger.info(s"${user}")

    repo.insert(user).map {
      case None => InternalServerError("Something bad happened!")
      case Some(code) => Ok(Json.toJson(code))
    }
  }

  def confirm(code: String) = Action.async { implicit request: Request[AnyContent] =>

    def confirm(status: Int, lastUpdate: Long): Future[Result] = {
      if(status != UserStatus.NOT_CONFIRMED){
        return Future.successful(NotFound(Json.obj(
          "error" -> JsString("User already confirmed!")
        )))
      }

      val now = System.currentTimeMillis()

      if(now - lastUpdate >= Constants.CODE_TTL){
        return Future.successful(Results.PreconditionFailed(Json.obj(
          "error" -> JsString("Code expired!")
        )))
      }

      repo.confirm(code).map { ok =>
        Results.Ok(Json.obj(
          "status" -> JsString("User confirmed successfully!")
        ))
      }
    }

    for {
      opt <- repo.getCodeInfo(code)
      result <- opt match {
        case Some(CodeInfo(id, _, lastUpdate, Some(status))) => confirm(status, lastUpdate)
        case _ => Future.successful(NotFound(s"Invalid confirmation code!"))
      }
    } yield {
      result
    }
  }

  def generateNewCode(login: String) = Action.async { implicit request: Request[AnyContent] =>
    repo.generateNewCode(login).map {
      case None => InternalServerError(Json.obj(
        "error" -> JsString("Something went wrong!")
      ))
      case Some(code) => Ok(Json.obj(
        "code" -> JsString(code)
      ))
    }
  }

  def logout() = Action { implicit request: Request[AnyContent] =>
    Ok("logged out").withNewSession
  }

  def login() = Action.async { implicit request: Request[AnyContent] =>

    val login = request.headers.get("login").get
    val password = request.headers.get("password").get

    logger.info(s"login ${login} password: ${password}\n")

    repo.getTokenByLogin(login, password).map {
      case None => Unauthorized("Login and/or password are wrong!")
      case Some(info) =>

        val sessionId = UUID.randomUUID.toString

        cache.put(info.id.toString, sessionId.getBytes())

        cache.put(sessionId, Json.toBytes(Json.toJson(SessionInfo(
          info.id.toString,
          info.login,
          info.token,
          info.expiresAt
        ))))

        Ok(Json.toJson(info)).withSession(
          "sessionId" -> sessionId
        )
    }
  }

  def generateNewToken() = Action.async { implicit request: Request[AnyContent] =>

    request.headers.get("refreshToken") match {
      case None => Future.successful(Forbidden(
        Json.obj(
          "error" -> JsString("Missing refreshToken header!")
        )
      ))
      case Some(refreshToken) =>

        repo.generateNewToken(refreshToken).map {
          case None => Forbidden(Json.obj(
            "error" -> JsString("Invalid refresh token or user not active!")
          ))
          case Some(info) =>

            val sessionId = UUID.randomUUID.toString

            cache.put(info.id.toString, sessionId.getBytes())

            cache.put(sessionId, Json.toBytes(Json.toJson(SessionInfo(
              info.id.toString,
              info.login,
              info.token,
              info.expiresAt
            ))))

            Ok(Json.toJson(info)).withSession(
              "sessionId" -> sessionId
            )
        }

    }
  }

  def update() = loginAction.async { implicit request: Request[AnyContent] =>
    val data = request.body.asJson.get.as[UserUpdate]
    val session = Json.parse(cache.get(request.session.data.get("sessionId").get).get).as[SessionInfo]
    val id = UUID.fromString(session.id)

    repo.update(id, data).map(ok => Ok(Json.obj(
      "ok" -> JsBoolean(ok)
    )))
  }

  def show() = loginAction { implicit request: Request[AnyContent] =>
    val id = request.session.data.get("sessionId").get

    cache.get(id) match {
      case None => InternalServerError(Json.obj(
        "error" -> JsString("Something went wrong!")
      ))
      case Some(info) => Ok(Json.parse(info))
    }
  }

}
