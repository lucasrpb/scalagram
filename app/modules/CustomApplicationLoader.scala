package modules

import play.api.inject.guice._
import play.api.{ApplicationLoader, Logging}

class CustomApplicationLoader () extends GuiceApplicationLoader() with Logging {
  override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
    //val extra = Configuration("a" -> 1)

    initialBuilder
      .in(context.environment)
      //.loadConfig(extra ++ context.initialConfiguration)
      .overrides(overrides(context): _*)
  }
}