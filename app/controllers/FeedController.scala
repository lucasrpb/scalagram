package controllers

import actions.LoginAction
import app.Cache
import play.api.mvc._
import repositories.FeedRepository

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


    Future.successful(Ok("ok"))
  }

}
