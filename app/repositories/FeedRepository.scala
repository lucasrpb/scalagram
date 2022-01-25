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

  override def unfollow(id: UUID, followerId: UUID): Future[Boolean] = {
    db.run(FollowerTable.followers.filter(f => f.userId === id && f.followerId === followerId).delete).map(_ == 1)
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
          u.username,
          p.followedAt
        )
      }
    }
  }

  override def getFollowerIds(id: UUID, lastId: Option[UUID], n: Int): Future[Seq[UUID]] = {
    if(lastId.isDefined) {
      return db.run(FollowerTable.followers.filter(f => f.userId === id && f.followerId > lastId.get.asColumnOf[UUID])
        .sortBy(_.followerId)
        .take(n)
        .result
      ).map(_.map(_.followerId))
    }

    db.run(FollowerTable.followers.filter(f => f.userId === id)
      .sortBy(_.followerId)
      .take(n)
      .result
    ).map(_.map(_.followerId))
  }

  override def insertPostIds(posts: Seq[Feed]): Future[Boolean] = {
    db.run((FeedTable.feeds ++= posts).transactionally).map(_.isDefined)
  }

  override def getFeedPosts(id: UUID, start: Int, n: Int, tags: List[String] = List.empty[String]): Future[Seq[PostDetailed]] = {

    logger.info(s"USER ID: ${id}, start: ${start} n: ${n}")

    if(!tags.isEmpty){
      return db.run(
        FeedTable.feeds.filter(_.followerId === id)
          .sortBy(_.postedAt.asc)
          .join(PostTable.posts).on{case (f, p) => f.postId === p.id && p.tags @& tags}
          .drop(start)
          .take(n)
          .join(UserTable.users).on{case ((f, p), u) => f.userId === u.id}
          .result
      ).map(_.map { case ((f, p), u) =>
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
      FeedTable.feeds.filter(_.followerId === id)
        .sortBy(_.postedAt.asc)
        .join(PostTable.posts).on{case (f, p) => f.postId === p.id}
        .drop(start)
        .take(n)
        .join(UserTable.users).on{case ((f, p), u) => f.userId === u.id}
        .result
    ).map(_.map { case ((f, p), u) =>
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
}
