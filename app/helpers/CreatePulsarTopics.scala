package helpers

import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.common.policies.data.RetentionPolicies
import org.slf4j.LoggerFactory

object CreatePulsarTopics {

  val PULSAR_SERVICE_URL = "pulsar://localhost:6650"
  val PULSAR_CLIENT_URL = "http://localhost:8080"

  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

    // Pass auth-plugin class fully-qualified name if Pulsar-security enabled
    val authPluginClassName = "pulsar"
    // Pass auth-param if auth-plugin class requires it
    val authParams = "param1=value1"
    val useTls = false
    val tlsAllowInsecureConnection = true
    val tlsTrustCertsFilePath = null
    val admin = PulsarAdmin.builder()
      //authentication(authPluginClassName, authParams)
      .serviceHttpUrl(PULSAR_CLIENT_URL)
      .tlsTrustCertsFilePath(tlsTrustCertsFilePath)
      .allowTlsInsecureConnection(tlsAllowInsecureConnection).build()

    try {

      //admin.namespaces().createNamespace("public/scalagram")
      admin.namespaces().setRetention("public/scalagram", new RetentionPolicies(-1, -1))
      admin.topics().createNonPartitionedTopic("feed-jobs")

    } catch {
      case t: Throwable =>
        t.printStackTrace()
    } finally {
      admin.close()
    }
  }

}
