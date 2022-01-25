package models

import play.api.libs.json.{Json, Reads, Writes}
import java.util.UUID

case class Feed(postId: UUID,
                userId: UUID,
                postedBy: UUID,
                postedAt: Long)

object Feed {
  implicit val feedFormat = Json.using[Json.WithDefaultValues].format[Feed]
  implicit val feedSeqReads = Reads.seq(feedFormat)
  implicit val feedSeqWrites = Writes.seq(feedFormat)
}
