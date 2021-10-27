package models

import play.api.libs.json.{Json, Reads, Writes}
import java.util.UUID

case class FeedJob(postId: UUID,
                   fromUserId: UUID,
                   postedAt: Long,
                   followers: Seq[UUID],
                   lastId: Option[UUID])

object FeedJob {
  implicit val feedTaskFormat = Json.using[Json.WithDefaultValues].format[FeedJob]
}
