package connections

import org.postgresql.ds.PGSimpleDataSource
import play.api.inject.ApplicationLifecycle
import slick.jdbc.JdbcBackend.Database

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostgresConnection @Inject ()(implicit ec: ExecutionContext, val lifecycle: ApplicationLifecycle) {

  protected val ds = new PGSimpleDataSource()

  ds.setURL("jdbc:postgresql://45fbc0f7-1c77-4a28-bd99-7e09e41ee965.gcp.ybdb.io:5433/postgres?ssl=true&sslmode=verify-full&sslrootcert=./root.crt")
  ds.setUser("admin")
  ds.setPassword("j4aQyOhEFLV9RvfPoh5l7eZ9607vsO")

  val db = Database.forDataSource(ds, None)

  lifecycle.addStopHook { () =>
    Future.successful(db.close())
  }
}
