package repositories

import com.google.inject.ImplementedBy
import connections.PostgresConnection
import models.slickmodels.{CommentTable, PostTable, UserTable}
import models.{Comment, CommentDetailed, Post}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import repositories.MyPostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[PostRepositoryImpl])
trait PostRepository {
  def insert(id: UUID, post: Post): Future[Boolean]
  def getPostsByUserId(id: UUID, start: Int, n: Int, tags: List[String] = List.empty[String]): Future[Seq[Post]]
  def insertComment(comment: Comment): Future[Boolean]
  def getComments(postId: UUID, start: Int, n: Int): Future[Seq[CommentDetailed]]
  def updateComment(commentId: UUID, body: String): Future[Boolean]
}

@Singleton
class PostRepositoryImpl @Inject ()(implicit val ec: ExecutionContext,
                                    val lifecycle: ApplicationLifecycle,
                                    val postgresConnection: PostgresConnection)
  extends PostRepository with Logging {

  import postgresConnection._

  override def insert(id: UUID, post: Post): Future[Boolean] = {
    db.run(PostTable.posts += post).map(_ == 1)
  }

  override def getPostsByUserId(id: UUID, start: Int = 0, n: Int, tags: List[String] = List.empty[String]): Future[Seq[Post]] = {
    if(!tags.isEmpty){
      return db.run(PostTable.posts.filter(p => p.userId === id && p.tags @& tags)
        .sortBy(_.postedAt.desc)
        .drop(start)
        .take(n)
        .result
      )
    }

    db.run(PostTable.posts.filter(p => p.userId === id)
      .sortBy(_.postedAt.desc)
      .drop(start)
      .take(n)
      .result
    )
  }

  override def insertComment(comment: Comment): Future[Boolean] = {
    db.run(CommentTable.comments += comment).map(_ == 1)
  }



  override def getComments(id: UUID, start: Int, n: Int): Future[Seq[CommentDetailed]] = {
    db.run(CommentTable.comments.filter(p => p.postId === id)
      .join(UserTable.users).on(_.userId === _.id)
      .sortBy(_._1.postedAt.desc)
      .drop(start)
      .take(n)
      .result
    ).map { comments =>
      comments.map { case (c, u) =>
        CommentDetailed(
          c.id,
          c.userId,
          u.username,
          c.body,
          c.postedAt,
          c.lastUpdateAt
        )
      }
    }
  }

  override def updateComment(commentId: UUID, body: String): Future[Boolean] = {
    db.run(
      CommentTable.comments.filter(_.id === commentId).map(_.body).update(body)
    ).map(_ == 1)
  }

}
