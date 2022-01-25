package models

import play.api.libs.json.Json

case class SessionInfo(id: String, login: String, token: String, expiresAt: Long)

object SessionInfo {
  implicit val tokenInfoFormat = Json.format[SessionInfo]
}


