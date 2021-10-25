package repositories

import com.google.inject.ImplementedBy
import models.slickmodels.FollowerTable
import play.api.inject.ApplicationLifecycle
import slick.jdbc.PostgresProfile.api._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[PostRepositoryImpl])
trait PostRepository {

}

@Singleton
class PostRepositoryImpl @Inject ()(implicit val ec: ExecutionContext, lifecycle: ApplicationLifecycle) extends PostRepository {
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

}
