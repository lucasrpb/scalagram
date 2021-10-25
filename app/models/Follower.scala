package models

import play.api.libs.json.Json
import java.util.UUID

case class Follower(userId: UUID,
                    followerId: UUID,
                    followedAt: Long = System.currentTimeMillis()
                   )

object Follower {
  implicit val userFormat = Json.using[Json.WithDefaultValues].format[Follower]
}



