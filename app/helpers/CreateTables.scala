package helpers

import models.slickmodels._
import org.postgresql.ds.PGSimpleDataSource
import play.api.{Environment, Logging}
import repositories.MyPostgresProfile.api._
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CreateTables extends Logging {

  def main(args: Array[String]): Unit = {

    val config = play.api.Configuration.load(Environment.simple())

    val ds = new PGSimpleDataSource()

    ds.setURL(config.get[String]("postgres.url"))
    ds.setUser(config.get[String]("postgres.user"))
    ds.setPassword(config.get[String]("postgres.password"))

    val db = Database.forDataSource(ds, None)

    val actions = DBIO.seq(
      /*UserTable.users.schema.createIfNotExists,
      ProfileTable.profiles.schema.createIfNotExists,
      PostTable.posts.schema.createIfNotExists,
      FeedTable.feeds.schema.createIfNotExists,
      FollowerTable.followers.schema.createIfNotExists*/

      PostTable.posts.schema.createIfNotExists
    )

    try {
      Await.result(db.run(actions.transactionally), Duration.Inf)
      logger.info(s"Tables created successfully!")
    } catch {
      case t: Throwable => logger.error(t.getMessage)
    } finally {
      db.close()
    }
  }

}
