package models.slickmodels

import models.Follower
import repositories.MyPostgresProfile.api._

import java.util.UUID

// Definition of the SUPPLIERS table
class FollowerTable(tag: Tag) extends Table[Follower](tag, "followers") {
  def userId = column[UUID]("user_id") // This is the primary key column
  def followeeId = column[UUID]("followee_id") // This is the primary key column
  def followedAt = column[Long]("followedAt")

  def pk = primaryKey("followers_pk", (userId, followeeId))
  def userIdFK = foreignKey("followers_user_id_fk", userId, UserTable.users)(_.id)
  def followerIdFK = foreignKey("followers_followee_id_fk", followeeId, UserTable.users)(_.id)

  def * = (userId, followeeId, followedAt) <> ((Follower.apply _).tupled, Follower.unapply)
}

object FollowerTable {
  val followers = TableQuery[FollowerTable]
}






