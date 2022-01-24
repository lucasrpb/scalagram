package connections

import com.sksamuel.pulsar4s.{PulsarClient, PulsarClientConfig}
import config.PulsarConfig
import org.apache.pulsar.client.api.AuthenticationFactory
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PulsarConnection @Inject() (val playConfig: Configuration, val lifecycle: ApplicationLifecycle) {

  val pulsarConfig: PulsarConfig = playConfig.get[PulsarConfig]("pulsar")

  val PULSAR_SERVICE_URL = pulsarConfig.serviceURL
  val TOKEN = pulsarConfig.token

  val config = PulsarClientConfig(
    serviceUrl = PULSAR_SERVICE_URL,
    allowTlsInsecureConnection = Some(false),
    authentication = Some(AuthenticationFactory.token(TOKEN))
  )

  val client = PulsarClient(config)

  lifecycle.addStopHook { () =>
    Future.successful(client.close())
  }

}
