package app

import config.ConstantsConfig
import play.api.{Environment, Logging}

object Constants extends Logging {

  val playConfig = play.api.Configuration.load(Environment.simple())
  val config: ConstantsConfig = playConfig.get[ConstantsConfig]("constants")

  val TOKEN_TTL = config.TOKEN_TTL
  val CODE_TTL = config.CODE_TTL
  val IMG_UPLOAD_FOLDER = config.IMG_UPLOAD_FOLDER
  val MAX_FOLLOWERS_POLL = config.MAX_FOLLOWERS_POLL

  logger.info(s"${Console.BLUE_B}CONSTANTS CONFIG: ${config}${Console.RESET}")

}
