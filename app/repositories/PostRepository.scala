package repositories

import com.google.inject.ImplementedBy
import models.Post
import models.slickmodels.{FollowerTable, PostTable}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[PostRepositoryImpl])
trait PostRepository {
  def insert(id: UUID, post: Post): Future[Boolean]
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
}
