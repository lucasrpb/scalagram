package config

import com.typesafe.config.Config
import play.api.ConfigLoader

import javax.inject.Singleton

case class PulsarConfig(serviceURL: String,
                        clientURL: String,
                        namespace: String,
                        jobsTopic: String,
                        feedTopic: String
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
        jobsTopic = config.getString("jobs-topic"),
        feedTopic = config.getString("feed-topic")
      )
    }
  }

}
