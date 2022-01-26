package models

import play.api.libs.json.{Json, Reads, Writes}
import java.util.UUID

case class Comment(id: UUID,
                   commentId: UUID,
                   userId: UUID,
                   body: String,
                   postedAt: Long = System.currentTimeMillis(),
                   lastUpdateAt: Long = System.currentTimeMillis()
                  )

object Comment {
  implicit val commentFormat = Json.using[Json.WithDefaultValues].format[Comment]
  implicit val commentSeqReads = Reads.seq(commentFormat)
  implicit val commentSeqWrites = Writes.seq(commentFormat)
}

