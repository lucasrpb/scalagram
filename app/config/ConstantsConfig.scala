package config

import com.typesafe.config.Config
import play.api.ConfigLoader

import javax.inject.Singleton

case class ConstantsConfig(
                            TOKEN_TTL: Long,
                            CODE_TTL: Long,
                            IMG_UPLOAD_FOLDER: String,
                            MAX_FOLLOWERS_POLL: Int
                          )

@Singleton
object ConstantsConfig {
  implicit val configLoader: ConfigLoader[ConstantsConfig] = new ConfigLoader[ConstantsConfig] {
    def load(rootConfig: Config, path: String): ConstantsConfig = {
      val config = rootConfig.getConfig(path)
      ConstantsConfig(
        TOKEN_TTL = config.getLong("tokenTTL"),
        CODE_TTL = config.getLong("codeTTL"),
        IMG_UPLOAD_FOLDER = config.getString("imgUploadFolder"),
        MAX_FOLLOWERS_POLL = config.getInt("maxFollowersPoll")
      )
    }
  }
}


