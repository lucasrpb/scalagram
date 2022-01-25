package modules

import play.api.Logging
import services.{FeedJobHandler, FeedService, ImageService}

import javax.inject.{Inject, Singleton}

@Singleton
class StartUpServices @Inject ()(val feedHandler: FeedJobHandler,
                                 val feedService: FeedService,
                                 val imageService: ImageService
                                ) extends Logging {

  logger.info(s"${Console.GREEN_B}Starting up services...${Console.RESET}")

}
