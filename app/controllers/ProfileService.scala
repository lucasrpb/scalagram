package controllers

import play.api.mvc._
import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProfileService @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

}
