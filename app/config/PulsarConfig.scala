package config

import com.typesafe.config.Config
import play.api.ConfigLoader

import javax.inject.Singleton

case class PulsarConfig(serviceURL: String,
                        clientURL: String,
                        namespace: String,
                        topic: String
                       )

@Singleton
object PulsarConfig {
  implicit val configLoader: ConfigLoader[PulsarConfig] = new ConfigLoader[PulsarConfig] {
    def load(rootConfig: Config, path: String): PulsarConfig = {
      val config = rootConfig.getConfig(path)
      PulsarConfig(
        serviceURL = config.getString("serviceURL"),
        clientURL = config.getString("clientURL"),
        namespace = config.getString("namespace"),
        topic = config.getString("topic")
      )
    }
  }
}
