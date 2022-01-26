package repositories

import com.google.inject.ImplementedBy
import connections.PostgresConnection
import models.{Comment, CommentDetailed, Feed, Follower, FollowerDetailed, Post, PostDetailed}
import models.slickmodels.{CommentTable, FeedTable, FollowerTable, PostTable, UserTable}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import repositories.MyPostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[FeedRepositoryImpl])
trait FeedRepository {

  def follow(id: UUID, data: Follower): Future[Boolean]
  def unfollow(id: UUID, followerId: UUID): Future[Boolean]

  // Get the users the provided id follows
  def getFollowing(id: UUID, start: Int, n: Int): Future[Seq[FollowerDetailed]]
  def getFollowingIds(id: UUID, lastId: Option[UUID], n: Int): Future[Seq[UUID]]

  // Get user who follow the provided id
  def getFollowers(id: UUID, start: Int, n: Int): Future[Seq[FollowerDetailed]]
  def getFollowerIds(id: UUID, lastId: Option[UUID], n: Int): Future[Seq[UUID]]

  def insertPostIds(posts: Seq[Feed]): Future[Boolean]

  def getFeedPosts(id: UUID, start: Int, n: Int, tags: List[String] = List.empty[String]): Future[Seq[PostDetailed]]
}

@Singleton
class FeedRepositoryImpl @Inject ()(implicit val ec: ExecutionContext,
                                    val lifecycle: ApplicationLifecycle,
                                    val postgresConnection: PostgresConnection)
  extends FeedRepository with Logging {

  import postgresConnection._

  override def follow(id: UUID, data: Follower): Future[Boolean] = {
    val action = for {
      exists <- FollowerTable.followers.filter(u => u.userId === id && u.followeeId === data.followeeId).exists.result
      result <- exists match {
        case true => DBIO.successful(0)
        case false => FollowerTable.followers += data
      }
    } yield {
      result == 1
    }

    db.run(action)
  }

  override def unfollow(id: UUID, followerId: UUID): Future[Boolean] = {
    db.run(FollowerTable.followers.filter(f => f.userId === id && f.followeeId === followerId).delete).map(_ == 1)
  }

  override def getFollowing(id: UUID, start: Int, n: Int): Future[Seq[FollowerDetailed]] = {
    db.run(FollowerTable.followers.filter(_.userId === id)
      .join(UserTable.users).on{case (f, u) => f.followeeId === u.id}
      .sortBy(_._1.followeeId)
      .drop(start)
      .take(n)
      .result
    ).map { result =>
      result.map { case (p, u) =>
        FollowerDetailed(
          p.userId,
          p.followeeId,
          u.username,
          p.followedAt
        )
      }
    }
  }

  override def getFollowingIds(id: UUID, lastId: Option[UUID], n: Int): Future[Seq[UUID]] = {
    if(lastId.isDefined) {
      return db.run(FollowerTable.followers.filter(f => f.userId === id && f.followeeId > lastId.get.asColumnOf[UUID])
        .sortBy(_.followeeId)
        .take(n)
        .result
      ).map(_.map(_.followeeId))
    }

    db.run(FollowerTable.followers.filter(f => f.userId === id)
      .sortBy(_.followeeId)
      .take(n)
      .result
    ).map(_.map(_.followeeId))
  }

  override def insertPostIds(posts: Seq[Feed]): Future[Boolean] = {
    db.run((FeedTable.feeds ++= posts).transactionally).map(_.isDefined)
  }

  override def getFeedPosts(id: UUID, start: Int, n: Int, tags: List[String] = List.empty[String]): Future[Seq[PostDetailed]] = {

    logger.info(s"USER ID: ${id}, start: ${start} n: ${n}")

    if(!tags.isEmpty){
      return db.run(
        FeedTable.feeds.filter(_.userId === id)
          .sortBy(_.postedAt.asc)
          .join(PostTable.posts).on{case (f, p) => f.postId === p.id && p.tags @& tags}
          .drop(start)
          .take(n)
          .join(UserTable.users).on{case ((f, p), u) => p.userId === u.id}
          .join(FollowerTable.followers).on{case (((f, p), u), ft) => ft.userId === id && ft.followeeId === p.userId}
          .result
      ).map(_.map { case (((f, p), u), _) =>
        PostDetailed(
          p.id,
          p.userId,
          u.username,
          p.imgType,
          p.description,
          p.tags,
          p.postedAt,
          p.lastUpdateAt
        )
      })
    }

    db.run(
      FeedTable.feeds.filter(_.userId === id)
        .sortBy(_.postedAt.asc)
        .join(PostTable.posts).on{case (f, p) => f.postId === p.id}
        .drop(start)
        .take(n)
        .join(UserTable.users).on{case ((f, p), u) => p.userId === u.id}
        .join(FollowerTable.followers).on{case (((f, p), u), ft) => ft.userId === id && ft.followeeId === p.userId}
        .result
    ).map(_.map { case (((f, p), u), _) =>
      PostDetailed(
        p.id,
        p.userId,
        u.username,
        p.imgType,
        p.description,
        p.tags,
        p.postedAt,
        p.lastUpdateAt
      )
    })
  }

  override def getFollowers(id: UUID, start: Int, n: Int): Future[Seq[FollowerDetailed]] = {
    db.run(FollowerTable.followers.filter(_.followeeId === id)
      .join(UserTable.users).on{case (f, u) => f.userId === u.id}
      .sortBy(_._1.userId)
      .drop(start)
      .take(n)
      .result
    ).map { result =>
      result.map { case (p, u) =>
        FollowerDetailed(
          id,
          u.id,
          u.username,
          p.followedAt
        )
      }
    }
  }

  override def getFollowerIds(id: UUID, lastId: Option[UUID], n: Int): Future[Seq[UUID]] = {
    if(lastId.isDefined) {
      return db.run(FollowerTable.followers.filter(f => f.followeeId === id && f.userId > lastId.get.asColumnOf[UUID])
        .sortBy(_.userId)
        .take(n)
        .result
      ).map(_.map(_.userId))
    }

    db.run(FollowerTable.followers.filter(f => f.followeeId === id)
      .sortBy(_.userId)
      .take(n)
      .result
    ).map(_.map(_.userId))
  }
}
