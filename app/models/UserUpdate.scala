package models

import play.api.libs.json.Json

case class UserUpdate(username: Option[String],
                      email: Option[String],
                      phone: Option[String],
                      password: Option[String]
                     )

object UserUpdate {
  implicit val userFormat = Json.using[Json.WithDefaultValues].format[UserUpdate]
}




