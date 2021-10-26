package repositories

import com.google.inject.ImplementedBy
import models.{Feed, Follower, FollowerDetailed, Post}
import models.slickmodels.{FeedTable, FollowerTable, PostTable, UserTable}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[FeedRepositoryImpl])
trait FeedRepository {

  def follow(id: UUID, data: Follower): Future[Boolean]
  def getFollowers(id: UUID, start: Int, n: Int): Future[Seq[FollowerDetailed]]
  def getFollowerIds(id: UUID, start: Int, n: Int): Future[Seq[UUID]]

  def insertPostIds(posts: Seq[Feed]): Future[Boolean]

  def getFeedPosts(id: UUID, start: Int, n: Int): Future[Seq[Post]]

}

@Singleton
class FeedRepositoryImpl @Inject ()(implicit val ec: ExecutionContext, lifecycle: ApplicationLifecycle) extends FeedRepository {
  val db = Database.forConfig("postgres")

  lifecycle.addStopHook { () =>
    Future.successful(db.close())
  }

  val setup = DBIO.seq(
    FeedTable.feeds.schema.createIfNotExists,

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

  override def getFollowers(id: UUID, start: Int, n: Int): Future[Seq[FollowerDetailed]] = {
    db.run(FollowerTable.followers.filter(_.userId === id)
      .join(UserTable.users).on{case (p, u) => p.followerId === u.id}
      .sortBy(_._1.followerId)
      .drop(start)
      .take(n)
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

  override def getFollowerIds(id: UUID, start: Int, n: Int): Future[Seq[UUID]] = {
    db.run(FollowerTable.followers.filter(_.followerId === id)
      .sortBy(_.userId)
      .drop(start)
      .take(n)
      .result
    ).map(_.map(_.userId))
  }

  override def insertPostIds(posts: Seq[Feed]): Future[Boolean] = {
    db.run((FeedTable.feeds ++= posts).transactionally).map(_.isDefined)
  }

  override def getFeedPosts(id: UUID, start: Int, n: Int): Future[Seq[Post]] = {
    db.run(
      FeedTable.feeds.filter(_.followerId === id)
        .sortBy(_.postId)
        .drop(start)
        .take(n)
        .join(PostTable.posts).on{case (f, p) => f.postId === p.id}
        .result
    ).map(_.map(_._2))
  }
}
