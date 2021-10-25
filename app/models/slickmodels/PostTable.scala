package models.slickmodels

import models.Post
import repositories.MyPostgresProfile.api._
import slick.sql.SqlProfile.ColumnOption.SqlType

import java.util.UUID

class PostTable(tag: Tag) extends Table[Post](tag, "posts") {
  def id = column[UUID]("id", O.PrimaryKey) // This is the primary key column
  def userId = column[UUID]("user_id")
  def img = column[String]("img_type")
  def description = column[Option[String]]("description", SqlType("text"))
  def tags = column[List[String]]("tags")
  def postedAt = column[Long]("postedAt")

  def userIdFK = foreignKey("posts_user_id_fk", userId, UserTable.users)(_.id)

  def * = (id, userId, img, description, tags, postedAt) <> ((Post.apply _).tupled, Post.unapply)
}

object PostTable {
  val posts = TableQuery[PostTable]
}



