package controllers

import actions.LoginAction
import app.Cache
import play.api.mvc._
import repositories.PostRepository

import javax.inject._
import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class PostController @Inject()(val controllerComponents: ControllerComponents,
                               val repo: PostRepository,
                               val loginAction: LoginAction,
                               val cache: Cache,
                               implicit val ec: ExecutionContext) extends BaseController {
  
}
