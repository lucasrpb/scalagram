package models.slickmodels

import models.Profile
import repositories.MyPostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType
import slick.lifted.CaseClassShape

import java.util.UUID

// Definition of the SUPPLIERS table
class ProfileTable(tag: Tag) extends Table[Profile](tag, "profiles") {
  def userId = column[UUID]("user_id", O.PrimaryKey) // This is the primary key column
  def name = column[Option[String]]("name")
  def bio = column[Option[String]]("bio", SqlType("text"))
  def lastUpdate = column[Long]("last_update")

  def userIdFK = foreignKey("profiles_user_id_fk", userId, UserTable.users)(_.id)

  // Every table needs a * projection with the same type as the table's type parameter
  //def * = Profile.apply(name, bio, lastUpdate)

  def * = (userId, name, bio, lastUpdate) <> ((Profile.apply _).tupled, Profile.unapply)
}

object ProfileTable {
  val profiles = TableQuery[ProfileTable]
}




