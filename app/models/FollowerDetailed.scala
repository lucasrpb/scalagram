package models

import play.api.libs.json.{Json, Reads, Writes}

import java.util.UUID

case class FollowerDetailed(userId: UUID,
                            followeeId: UUID,
                            followeeUsername: String,
                            followedAt: Long = System.currentTimeMillis())


object FollowerDetailed {
  implicit val followerDetailedFormat = Json.using[Json.WithDefaultValues].format[FollowerDetailed]
  implicit val followerDetailedSeqReads = Reads.seq(followerDetailedFormat)
  implicit val followerDetailedSeqWrites = Writes.seq(followerDetailedFormat)
}

