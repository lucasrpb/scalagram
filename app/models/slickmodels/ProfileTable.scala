package models.slickmodels

import repositories.MyPostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

import java.util.UUID

// Definition of the SUPPLIERS table
class ProfileTable(tag: Tag) extends Table[(UUID, String, String, Long)](tag, "profiles") {
  def userId = column[UUID]("user_id", O.PrimaryKey) // This is the primary key column
  def name = column[String]("name")
  def bio = column[String]("bio", SqlType("text"))
  def lastUpdate = column[Long]("last_update")

  def userIdFK = foreignKey("profiles_user_id_fk", userId, UserTable.users)(_.id)

  // Every table needs a * projection with the same type as the table's type parameter
  def * = (userId, name, bio, lastUpdate)
}

object ProfileTable {
  val profiles = TableQuery[ProfileTable]
}




