package controllers

import play.api.mvc._
import repositories.ProfileRepository

import javax.inject._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ProfileController @Inject()(val controllerComponents: ControllerComponents,
                                  val repo: ProfileRepository
                                 ) extends BaseController {

}
