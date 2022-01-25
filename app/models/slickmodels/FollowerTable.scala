package models.slickmodels

import models.Follower
import repositories.MyPostgresProfile.api._

import java.util.UUID

// Definition of the SUPPLIERS table
class FollowerTable(tag: Tag) extends Table[Follower](tag, "followers") {
  def userId = column[UUID]("user_id") // This is the primary key column
  def followerId = column[UUID]("follower_id") // This is the primary key column
  def followedAt = column[Long]("followedAt")

  def pk = primaryKey("followers_pk", (userId, followerId))
  def userIdFK = foreignKey("followers_user_id_fk", userId, UserTable.users)(_.id)
  def followerIdFK = foreignKey("followers_follower_id_fk", followerId, UserTable.users)(_.id)

  def * = (userId, followerId, followedAt) <> ((Follower.apply _).tupled, Follower.unapply)
}

object FollowerTable {
  val followers = TableQuery[FollowerTable]
}






