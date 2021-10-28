package models

import play.api.libs.json.{Json, Reads, Writes}
import java.util.UUID

case class PostDetailed(id: UUID = UUID.randomUUID,
                        userId: UUID,
                        username: String,
                        imgType: String,
                        description: Option[String],
                        var tags: List[String],
                        postedAt: Long)

object PostDetailed {
  implicit val postDetailFormat = Json.using[Json.WithDefaultValues].format[PostDetailed]
  implicit val postDetailSeqReads = Reads.seq(postDetailFormat)
  implicit val postDetailSeqWrites = Writes.seq(postDetailFormat)
}

