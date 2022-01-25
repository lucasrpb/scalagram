package controllers

import actions.{AccessTokenAction, LoginAction}
import app.{Cache, Constants}
import models.{CodeInfo, SessionInfo, TokenInfo, User, UserStatus, UserUpdate}
import play.api.Logging
import play.api.libs.Codecs.sha1
import play.api.libs.json.{JsArray, JsBoolean, JsError, JsString, JsSuccess, Json}
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
                               val loginAction: LoginAction,
                               implicit val ec: ExecutionContext) extends BaseController with Logging {

  def insert() = Action.async { implicit request: Request[AnyContent] =>

    val validation = request.body.asJson.get.validate(User.userFormat)

    validation match {
      case JsError(errors) =>

        Future.successful(
          BadRequest(
            Json.toJson(errors.map{case (path, errors) => path.toString() -> errors.map(_.messages)}.toMap)
          )
        )

      case JsSuccess(value, path) =>

        val user = validation.get

        logger.info(s"${user}")

        repo.userExists(user.username, user.email, user.phone).flatMap {
          case false =>

            repo.insert(user).map { ok =>
              Ok(Json.toJson(ok))
            }

          case true => Future.successful(BadRequest(Json.obj(
              "error" -> JsString("Username, email or phone already exists!")
            ))
          )

        }.recover {
          case t: Throwable => InternalServerError("Something bad happened!")
        }

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

    val headers = request.headers
    val loginOpt = headers.get("login")
    val passwordOpt = headers.get("password")

    if(loginOpt.isEmpty || passwordOpt.isEmpty){
      Future.successful(
        Unauthorized("Missing header login and/or password information!")
      )
    } else {

      val login = loginOpt.get
      val password = sha1(passwordOpt.get)

      logger.info(s"login ${login} password: ${password}\n")

      repo.getTokenByLogin(login, password).flatMap {
        case None => Future.successful(Unauthorized("Login and/or password are wrong!"))
        case Some(info) =>

          val sessionId = UUID.randomUUID.toString

          for {
            _ <- cache.put(info.id.toString, sessionId.getBytes())

            _ <- cache.put(sessionId, Json.toBytes(Json.toJson(SessionInfo(
              info.id.toString,
              info.login,
              info.token,
              info.expiresAt
            ))))
          } yield {
            Ok(Json.toJson(info)).withSession(
              "sessionId" -> sessionId
            )
          }
      }

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

        repo.generateNewToken(refreshToken).flatMap {

          case None => Future.successful(Forbidden(Json.obj(
            "error" -> JsString("Invalid refresh token or user not active!")
          )))

          case Some(info) =>

            val sessionId = UUID.randomUUID.toString

            for {
              _ <- cache.put(info.id.toString, sessionId.getBytes())

              _ <- cache.put(sessionId, Json.toBytes(Json.toJson(SessionInfo(
                      info.id.toString,
                      info.login,
                      info.token,
                      info.expiresAt
              ))))
            } yield {
              Ok(Json.toJson(info)).withSession(
                "sessionId" -> sessionId
              )
            }
        }

    }
  }

  def update() = loginAction.async { implicit request: Request[AnyContent] =>
    cache.get(request.session.data.get("sessionId").getOrElse("")).flatMap {
      case None => Future.successful(InternalServerError(Json.obj(
        "error" -> JsString("Session not found!")
      )).withNewSession)

      case Some(bytes) =>

        val session = Json.parse(bytes).as[SessionInfo]

        val data = request.body.asJson.get.as[UserUpdate]
        val id = UUID.fromString(session.id)

        repo.update(id, data).map(ok => Ok(Json.obj(
          "ok" -> JsBoolean(ok)
        )))

    }
  }

}
