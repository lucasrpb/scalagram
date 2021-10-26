package repositories

import com.google.inject.ImplementedBy
import models.Post
import models.slickmodels.{FollowerTable, PostTable, UserTable}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[PostRepositoryImpl])
trait PostRepository {
  def insert(id: UUID, post: Post): Future[Boolean]
  def getPostsByUserId(id: UUID, start: Int, n: Int): Future[Seq[Post]]
}

@Singleton
class PostRepositoryImpl @Inject ()(implicit val ec: ExecutionContext, lifecycle: ApplicationLifecycle) extends PostRepository {
  val db = Database.forConfig("postgres")

  lifecycle.addStopHook { () =>
    Future.successful(db.close())
  }

  val setup = DBIO.seq(
    // Create the tables, including primary and foreign keys
    PostTable.posts.schema.createIfNotExists
  )

  val setupFuture = db.run(setup).onComplete {
    case Success(ok) => println(ok)
    case Failure(ex) => ex.printStackTrace()
  }

  override def insert(id: UUID, post: Post): Future[Boolean] = {
    db.run(PostTable.posts += post).map(_ == 1)
  }

  override def getPostsByUserId(id: UUID, start: Int = 0, n: Int): Future[Seq[Post]] = {
    db.run(PostTable.posts.filter(_.userId === id)
      .drop(start)
      .take(n)
      .sortBy(_.postedAt.desc)
      .result
    )
  }
}
