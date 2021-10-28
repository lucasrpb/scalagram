package controllers

import actions.LoginAction
import app.Cache
import models.SessionInfo

import javax.inject._
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import repositories.UserRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val repo: UserRepository,
                               val cache: Cache,
                               val loginAction: LoginAction,
                               implicit val ec: ExecutionContext
                              ) extends BaseController with Logging {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  protected def process(request: Request[AnyContent]): Future[Result] = {

    val login = request.headers.get("login").get
    val password = request.headers.get("password").get

    //logger.info(s"\n${Console.GREEN_B}data: ${request.body} type: ${request.contentType} json: ${data}${Console.RESET}\n")

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

  def login() = Action.async { implicit request: Request[AnyContent] =>
    process(request)
  }

  def process2(request: Request[AnyContent]): Future[Result] = {
    if(request.session.isEmpty || request.session.data.isEmpty) return Future.successful(
      BadRequest("session must not be null!")
    )

    val sessionId = request.session.data.get("sessionId")

    if(sessionId.isEmpty){
      return Future.successful(
        BadRequest("sessionId must not be null!")
      )
    }

    val opt = cache.get(sessionId.get)

    if(opt.isEmpty){
      return Future.successful(
        BadRequest("Session cache is null!")
      )
    }

    logger.info(s"${Console.GREEN_B}sessionId: ${Json.parse(opt.get).as[SessionInfo]}${Console.RESET}")

    Future.successful(Ok("you got it!"))
  }

  def action() = Action.async { implicit request: Request[AnyContent] =>
    process2(request)
  }
}
