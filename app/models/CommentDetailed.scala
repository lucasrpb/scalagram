package models

import play.api.libs.json.{Json, Reads, Writes}

import java.util.UUID

case class CommentDetailed(id: UUID = UUID.randomUUID,
                           userId: UUID,
                           username: String,
                           body: String,
                           postedAt: Long,
                           lastUpdateAt: Long
                          )

object CommentDetailed {
  implicit val commentDetailFormat = Json.using[Json.WithDefaultValues].format[CommentDetailed]
  implicit val commentDetailSeqReads = Reads.seq(commentDetailFormat)
  implicit val commentDetailSeqWrites = Writes.seq(commentDetailFormat)
}

