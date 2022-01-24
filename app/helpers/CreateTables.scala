package helpers

import models.slickmodels._
import org.postgresql.ds.PGSimpleDataSource
import play.api.Logging
import repositories.MyPostgresProfile.api._
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CreateTables extends Logging {

  def main(args: Array[String]): Unit = {

    val ds = new PGSimpleDataSource()

    ds.setURL("jdbc:postgresql://45fbc0f7-1c77-4a28-bd99-7e09e41ee965.gcp.ybdb.io:5433/postgres?ssl=true&sslmode=verify-full&sslrootcert=./root.crt")
    ds.setUser("admin")
    ds.setPassword("j4aQyOhEFLV9RvfPoh5l7eZ9607vsO")

    val db = Database.forDataSource(ds, None)

    val actions = DBIO.seq(
      /*UserTable.users.schema.createIfNotExists,
      ProfileTable.profiles.schema.createIfNotExists,
      PostTable.posts.schema.createIfNotExists,
      FeedTable.feeds.schema.createIfNotExists,
      FollowerTable.followers.schema.createIfNotExists*/

      CommentTable.comments.schema.createIfNotExists
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
