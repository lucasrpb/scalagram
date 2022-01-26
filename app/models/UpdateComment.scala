package models

import play.api.libs.json.{Json, Reads, Writes}

import java.util.UUID

case class UpdateComment(commentId: UUID,
                         userId: UUID,
                         body: String,
                         postedAt: Long = System.currentTimeMillis(),
                         lastUpdateAt: Long = System.currentTimeMillis())


object UpdateComment {
  implicit val ucFormat = Json.using[Json.WithDefaultValues].format[UpdateComment]
  implicit val ucSeqReads = Reads.seq(ucFormat)
  implicit val ucSeqWrites = Writes.seq(ucFormat)
}


