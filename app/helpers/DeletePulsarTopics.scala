package helpers

import org.apache.pulsar.client.admin.PulsarAdmin
import org.slf4j.LoggerFactory

object DeletePulsarTopics {

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

    //topics.createNonPartitionedTopic(s"persistent://public/default/log4")

    //topics.deletePartitionedTopic(Config.Topics.LOG)
    //admin.namespaces().createNamespace("public/darwindb")
    //admin.namespaces().setRetention("public/darwindb", new RetentionPolicies(-1, -1))
   // topics.createPartitionedTopic(Config.Topics.LOG, 1)
   // topics.createNonPartitionedTopic(Config.Topics.LOG)

    try {
      //admin.namespaces().deleteNamespace("public/scalagram")
      admin.topics().delete("feed-jobs", true)
    } catch {
      case t: Throwable =>
        t.printStackTrace()
    } finally {
      admin.close()
    }
  }

}
