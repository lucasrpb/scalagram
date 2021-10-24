package repositories

import app.Constants
import com.google.inject.ImplementedBy
import models.slickmodels.UserTable
import models.{TokenInfo, User, UserStatus}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.PostgresProfile.api._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@ImplementedBy(classOf[UserRepositoryImpl])
trait UserRepository {
  def insert(user: User): Future[Boolean]
  def getCodeInfo(code: String): Future[Option[(Int, Long)]]
  def confirm(code: String): Future[Boolean]

  def generateNewCode(login: String): Future[Option[String]]

  def getToken(token: String): Future[Option[TokenInfo]]

  def getTokenByLogin(login: String, password: String): Future[Option[TokenInfo]]

  def generateNewToken(refreshToken: String): Future[Option[TokenInfo]]
}

@Singleton
class UserRepositoryImpl @Inject ()(implicit val ec: ExecutionContext, lifecycle: ApplicationLifecycle) extends UserRepository {
  val db = Database.forConfig("postgres")

  lifecycle.addStopHook { () =>
    Future.successful(db.close())
  }

  val setup = DBIO.seq(
    // Create the tables, including primary and foreign keys
    UserTable.users.schema.createIfNotExists,

    // Insert some suppliers
    // UserTable.users += (UUID.randomUUID(), "lucasrpb", "lucasrpb@gmail.com")
  )

  val setupFuture = db.run(setup).onComplete {
    case Success(ok) => println(ok)
    case Failure(ex) => ex.printStackTrace()
  }

  override def insert(user: User): Future[Boolean] = {
    val op = UserTable.users += (user.id, user.username, user.password, user.email, user.phone,
      user.code, user.token, user.createdAt, user.codeLastUpdate, user.tokenLastUpdate, user.status, user.refreshToken)
    db.run(op).map(_ == 1)
  }

  override def getCodeInfo(code: String): Future[Option[(Int, Long)]] = {
    val op = UserTable.users.filter(_.code === code).map(u => u.status -> u.codeLastUpdate).result
    db.run(op).map(_.headOption)
  }

  override def confirm(code: String): Future[Boolean] = {
    val op = UserTable.users.filter(u => u.code === code).map(_.status).update(UserStatus.ACTIVE)
    db.run(op).map(_ == 1)
  }

  override def generateNewCode(login: String): Future[Option[String]] = {
    val code = UUID.randomUUID().toString
    val now = System.currentTimeMillis()

    val op = UserTable.users.filter(u => (u.username === login || u.email === login) &&
      u.status === UserStatus.NOT_CONFIRMED).map(s => s.code -> s.codeLastUpdate).update(code -> now)

    db.run(op).map(n => if(n == 1) Some(code) else None)
  }

  override def getToken(token: String): Future[Option[TokenInfo]] = {
    val op = UserTable.users.filter(u => u.token === token).result
    db.run(op).map(_.headOption.map(u => TokenInfo(u._7, u._12, u._2, u._1, u._10 + 3600L * 1000L)))
  }

  override def getTokenByLogin(login: String, password: String): Future[Option[TokenInfo]] = {
    val now = System.currentTimeMillis()
    val token = UUID.randomUUID().toString
    val refreshToken = UUID.randomUUID().toString
    val expiresAt = now + Constants.TOKEN_TTL

   val action = (for {
     sel <- UserTable.users.filter(u => (u.username === login || u.email === login) &&
                                         u.status === UserStatus.ACTIVE && u.password === password).result
     n <- if(!sel.isEmpty) UserTable.users.filter(_.id === sel.head._1.asColumnOf[UUID])
       .map(u => u.tokenLastUpdate -> u.token -> u.refreshToken)
       .update(now -> token -> refreshToken) else DBIO.successful(0)
    } yield if(n == 1) Some(TokenInfo(token, refreshToken, login, sel.head._1, expiresAt)) else None)

    db.run(action)
  }

  override def generateNewToken(refreshToken: String): Future[Option[TokenInfo]] = {
    val token = UUID.randomUUID().toString
    val newRefreshToken = UUID.randomUUID().toString
    val now = System.currentTimeMillis()
    val expiresAt = now + Constants.TOKEN_TTL

    val action = (for {
      sel <- UserTable.users.filter(u => (u.refreshToken === refreshToken) && u.status === UserStatus.ACTIVE).result
      n <- if(!sel.isEmpty) UserTable.users.filter(_.id === sel.head._1.asColumnOf[UUID])
        .map(u => u.tokenLastUpdate -> u.token -> u.refreshToken)
        .update(now -> token -> newRefreshToken) else DBIO.successful(0)
    } yield if(n == 1) Some(TokenInfo(token, refreshToken, sel.head._2, sel.head._1, expiresAt)) else None)

    db.run(action)
  }
}
