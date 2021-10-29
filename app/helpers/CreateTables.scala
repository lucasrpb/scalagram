package helpers

import models.slickmodels._
import play.api.Logging
import repositories.MyPostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CreateTables extends Logging {

  def main(args: Array[String]): Unit = {

    val db = Database.forConfig("postgres")

    val actions = DBIO.seq(
      UserTable.users.schema.createIfNotExists,
      ProfileTable.profiles.schema.createIfNotExists,
      PostTable.posts.schema.createIfNotExists,
      FeedTable.feeds.schema.createIfNotExists,
      FollowerTable.followers.schema.createIfNotExists
    )

    try{
      Await.result(db.run(actions.transactionally), Duration.Inf)
      logger.info(s"Tables created successfully!")
    } catch {
      case t: Throwable => logger.error(t.getMessage)
    } finally {
      db.close()
    }
  }

}
