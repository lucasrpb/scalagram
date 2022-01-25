package models.slickmodels

import models.{Feed, Follower}
import repositories.MyPostgresProfile.api._

import java.util.UUID

// Definition of the SUPPLIERS table
class FeedTable(tag: Tag) extends Table[Feed](tag, "feeds") {
  def userId = column[UUID]("user_id") // This is the primary key column
  def followerId = column[UUID]("follower_id") // This is the primary key column
  def postId = column[UUID]("post_id")
  def postedAt = column[Long]("posted_at")

  def pk = primaryKey("feed_pk", (userId, followerId, postId))

  def userIdFK = foreignKey("feeds_user_id_fk", userId, UserTable.users)(_.id)
  def followerIdFK = foreignKey("feeds_follower_id_fk", followerId, UserTable.users)(_.id)
  def postIdFK = foreignKey("feeds_post_id_fk", postId, PostTable.posts)(_.id)

  def * = (userId, followerId, postId, postedAt) <> ((Feed.apply _).tupled, Feed.unapply)
}

object FeedTable {
  val feeds = TableQuery[FeedTable]
}







