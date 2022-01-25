package models

import play.api.libs.json.{Json, Reads, Writes}
import java.util.UUID

case class Follower(userId: UUID,
                    followerId: UUID,
                    followedAt: Long = System.currentTimeMillis()
                   )

object Follower {
  implicit val followerFormat = Json.using[Json.WithDefaultValues].format[Follower]
  implicit val followerSeqReads = Reads.seq(followerFormat)
  implicit val followerSeqWrites = Writes.seq(followerFormat)
}



