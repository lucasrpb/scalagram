package helpers

import config.PulsarConfig
import models.slickmodels._
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.common.policies.data.RetentionPolicies
import play.api.{Environment, Logging}
import repositories.MyPostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import scala.collection.JavaConverters._
import repositories.MyPostgresProfile.api._

object Setup extends Logging {

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

    val topics = admin.topics()
    val namespaces = admin.namespaces()

    val db = Database.forConfig("postgres")

    try {

      val existingNamespaces = namespaces.getNamespaces("public").asScala

      if(!existingNamespaces.contains("public/scalagram")){
        namespaces.createNamespace(pulsarConfig.namespace)
        namespaces.setRetention(pulsarConfig.namespace, new RetentionPolicies(-1, -1))
      }

      val existingTopics = topics.getList(pulsarConfig.namespace).asScala

      // Recreate topics
      existingTopics.foreach {topics.delete(_)}

      topics.createNonPartitionedTopic(pulsarConfig.jobsTopic)
      topics.createNonPartitionedTopic(pulsarConfig.feedTopic)

      val actions = DBIO.seq(
        UserTable.users.schema.createIfNotExists,
        ProfileTable.profiles.schema.createIfNotExists,
        PostTable.posts.schema.createIfNotExists,
        FeedTable.feeds.schema.createIfNotExists,
        FollowerTable.followers.schema.createIfNotExists
      )

      Await.result(db.run(actions), Duration.Inf)

    } catch {
      case t: Throwable => logger.error(t.getMessage)
    } finally {
      admin.close()
      db.close()
    }

  }

}
