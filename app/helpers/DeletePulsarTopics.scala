package helpers

import config.PulsarConfig
import org.apache.pulsar.client.admin.PulsarAdmin
import org.slf4j.LoggerFactory
import play.api.Environment

object DeletePulsarTopics {

  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

    val playConfig = play.api.Configuration.load(Environment.simple())
    val pulsarConfig: PulsarConfig = playConfig.get[PulsarConfig]("pulsar")

    // Pass auth-plugin class fully-qualified name if Pulsar-security enabled
    val authPluginClassName = "pulsar"
    // Pass auth-param if auth-plugin class requires it
    val authParams = "param1=value1"
    val useTls = false
    val tlsAllowInsecureConnection = true
    val tlsTrustCertsFilePath = null
    val admin = PulsarAdmin.builder()
      //authentication(authPluginClassName, authParams)
      .serviceHttpUrl(pulsarConfig.clientURL)
      .tlsTrustCertsFilePath(tlsTrustCertsFilePath)
      .allowTlsInsecureConnection(tlsAllowInsecureConnection).build()

    try {

      val topics = admin.topics().getList(pulsarConfig.namespace)

      //admin.namespaces().deleteNamespace(pulsarConfig.namespace)
      admin.topics().delete(pulsarConfig.jobsTopic, true)
      admin.topics().delete(pulsarConfig.feedTopic, true)
    } catch {
      case t: Throwable =>
        t.printStackTrace()
    } finally {
      admin.close()
    }
  }

}
