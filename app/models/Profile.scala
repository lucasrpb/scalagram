package models

import play.api.libs.json.Json
import java.util.UUID

case class Profile(userId: UUID,
                   name: Option[String] = Some(""),
                   bio: Option[String] = Some(""),
                   lastUpdate: Long = System.currentTimeMillis())

object Profile {
  implicit val profileFormat = Json.using[Json.WithDefaultValues].format[Profile]
}

