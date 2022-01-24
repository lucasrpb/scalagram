package models.slickmodels

//import slick.jdbc.PostgresProfile.api._
//import com.github.tminglei.slickpg._
import models.{Profile, User}
import repositories.MyPostgresProfile.api._

import java.util.UUID

// Definition of the SUPPLIERS table
class UserTable(tag: Tag) extends Table[User](tag, "users") {
  def id = column[UUID]("id", O.PrimaryKey) // This is the primary key column
  def username = column[String]("username", O.Unique)
  def password = column[String]("password")
  def email = column[String]("email", O.Unique)
  def phone = column[String]("phone", O.Unique)
  def code = column[String]("confirmation_code", O.Unique)
  def token = column[String]("token", O.Unique)
  def createdAt = column[Long]("createdAt")
  def codeLastUpdate = column[Long]("code_last_update")
  def tokenLastUpdate = column[Long]("token_last_update")
  def status = column[Int]("status")
  def refreshToken = column[String]("refresh_token", O.Unique)
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id, username, password, email, phone, code, token, refreshToken, createdAt, tokenLastUpdate, codeLastUpdate, status) <> ((User.apply _).tupled, User.unapply)
}

object UserTable {
  val users = TableQuery[UserTable]
}
