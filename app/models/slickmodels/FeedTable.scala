package models.slickmodels

import models.{Feed, Follower}
import repositories.MyPostgresProfile.api._

import java.util.UUID

// Definition of the SUPPLIERS table
class FeedTable(tag: Tag) extends Table[Feed](tag, "feeds") {
  def postId = column[UUID]("postId") // This is the primary key column
  def userId = column[UUID]("userId") // This is the primary key column
  def postedBy = column[UUID]("postedBy") // This is the primary key column
  def postedAt = column[Long]("posted_at")

  def pk = primaryKey("feed_pk", (postId, userId))

  def userIdFK = foreignKey("feeds_user_id_fk", userId, UserTable.users)(_.id)
  def postIdFK = foreignKey("feeds_post_id_fk", postId, PostTable.posts)(_.id)

  def * = (postId, userId, postedBy, postedAt) <> ((Feed.apply _).tupled, Feed.unapply)
}

object FeedTable {
  val feeds = TableQuery[FeedTable]
}







