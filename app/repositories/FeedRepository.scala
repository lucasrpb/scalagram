package repositories

import com.google.inject.ImplementedBy
import models.{Follower, FollowerDetailed}
import models.slickmodels.{FollowerTable, UserTable}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[FeedRepositoryImpl])
trait FeedRepository {

  def follow(id: UUID, data: Follower): Future[Boolean]
  def getFollowers(id: UUID, start: Int = 0): Future[Seq[FollowerDetailed]]

}

@Singleton
class FeedRepositoryImpl @Inject ()(implicit val ec: ExecutionContext, lifecycle: ApplicationLifecycle) extends FeedRepository {
  val db = Database.forConfig("postgres")

  lifecycle.addStopHook { () =>
    Future.successful(db.close())
  }

  val setup = DBIO.seq(
    // Create the tables, including primary and foreign keys
    FollowerTable.followers.schema.createIfNotExists
  )

  val setupFuture = db.run(setup).onComplete {
    case Success(ok) => println(ok)
    case Failure(ex) => ex.printStackTrace()
  }

  override def follow(id: UUID, data: Follower): Future[Boolean] = {
    val action = for {
      exists <- FollowerTable.followers.filter(u => u.userId === id && u.followerId ===data.followerId).exists.result
      result <- exists match {
        case true => DBIO.successful(0)
        case false => FollowerTable.followers += data
      }
    } yield {
      result == 1
    }

    db.run(action)
  }

  override def getFollowers(id: UUID, start: Int = 0): Future[Seq[FollowerDetailed]] = {
    db.run(FollowerTable.followers.filter(_.userId === id)
      .join(UserTable.users).on{case (p, u) => p.followerId === u.id}
      .drop(start).take(2)
      .result
    ).map { result =>
      result.map { case (p, u) =>
        FollowerDetailed(
          p.userId,
          p.followerId,
          u._2,
          p.followedAt
        )
      }
    }
  }

}
