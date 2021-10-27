package modules

import play.api.Logging
import services.{FeedJobHandler, FeedService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class StartUpServices @Inject ()(val feedHandler: FeedJobHandler,
                                 val feedService: FeedService
                                ) extends Logging {

  logger.info(s"${Console.GREEN_B}Starting up services...${Console.RESET}")

}
