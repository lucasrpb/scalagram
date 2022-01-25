package helpers

import config.PulsarConfig
import models.slickmodels._
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.api.AuthenticationFactory
import org.apache.pulsar.common.policies.data.RetentionPolicies
import org.postgresql.ds.PGSimpleDataSource
import play.api.{Environment, Logging}
import repositories.MyPostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import scala.collection.JavaConverters._
import repositories.MyPostgresProfile.api._
import slick.jdbc.JdbcBackend.Database

object Setup extends Logging {

  def main(args: Array[String]): Unit = {

    val config = play.api.Configuration.load(Environment.simple())

    val pulsarConfig: PulsarConfig = config.get[PulsarConfig]("pulsar")

    val admin = PulsarAdmin.builder()
      .authentication(AuthenticationFactory.token(pulsarConfig.token))
      .serviceHttpUrl(pulsarConfig.clientURL)
      .build()

    val topics = admin.topics()
    val namespaces = admin.namespaces()

    val ds = new PGSimpleDataSource()

    ds.setURL(config.get[String]("postgres.url"))
    ds.setUser(config.get[String]("postgres.user"))
    ds.setPassword(config.get[String]("postgres.password"))

    val db = Database.forDataSource(ds, None)

    try {

      val existingNamespaces = namespaces.getNamespaces("scalagram-app").asScala

      if(!existingNamespaces.contains(pulsarConfig.namespace)){
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
