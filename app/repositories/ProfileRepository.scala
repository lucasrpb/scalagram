package repositories

import com.google.inject.ImplementedBy
import models.Profile
import models.slickmodels.ProfileTable
import play.api.inject.ApplicationLifecycle
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[ProfileRepositoryImpl])
trait ProfileRepository {
  def upsert(id: UUID, profile: Profile): Future[Boolean]
}

@Singleton
class ProfileRepositoryImpl @Inject ()(implicit val ec: ExecutionContext, lifecycle: ApplicationLifecycle) extends ProfileRepository {
  val db = Database.forConfig("postgres")

  lifecycle.addStopHook { () =>
    Future.successful(db.close())
  }

  val setup = DBIO.seq(
    // Create the tables, including primary and foreign keys
    ProfileTable.profiles.schema.createIfNotExists
  )

  val setupFuture = db.run(setup).onComplete {
    case Success(ok) => println(ok)
    case Failure(ex) => ex.printStackTrace()
  }

  override def upsert(id: UUID, profile: Profile): Future[Boolean] = {
    val action = for {
      n <- ProfileTable.profiles.filter(_.userId === id).map(p => p.name -> p.bio).update(profile.name -> profile.bio)
      result <- n match {
        case 0 => ProfileTable.profiles += profile
        case 1 => DBIO.successful(1)
        case _ => DBIO.failed(new RuntimeException(s"Error updating profile for user ${id}!"))
      }
    } yield {
      result == 1
    }

    db.run(action)
  }
}
