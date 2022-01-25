package models.slickmodels

import models.Comment
import repositories.MyPostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

import java.util.UUID

class CommentTable(tag: Tag) extends Table[Comment](tag, "post_comments") {
  def id = column[UUID]("id", O.PrimaryKey) // This is the primary key column
  def postId = column[UUID]("post_id")
  def userId = column[UUID]("user_id")
  def body = column[String]("body", SqlType("text"))
  def postedAt = column[Long]("postedAt")
  def lastUpdateAt = column[Long]("lastUpdateAt")

  def * = (id, postId, userId, body, postedAt, lastUpdateAt) <> ((Comment.apply _).tupled, Comment.unapply)
}

object CommentTable {
  val comments = TableQuery[CommentTable]
}






