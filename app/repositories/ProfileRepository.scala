package repositories

import com.google.inject.ImplementedBy
import models.Profile
import models.slickmodels.ProfileTable
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[ProfileRepositoryImpl])
trait ProfileRepository {
  def upsert(id: UUID, profile: Profile): Future[Boolean]
  def get(id: UUID): Future[Option[Profile]]
}

@Singleton
class ProfileRepositoryImpl @Inject ()(implicit val ec: ExecutionContext, lifecycle: ApplicationLifecycle)
  extends ProfileRepository with Logging {
  val db = Database.forConfig("postgres")

  lifecycle.addStopHook { () =>
    Future.successful(db.close())
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

  override def get(id: UUID): Future[Option[Profile]] = {
    db.run(ProfileTable.profiles.filter(_.userId === id).result.map(_.headOption))
  }
}
