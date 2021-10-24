package models

import play.api.libs.json.Json
import java.util.UUID

case class TokenInfo(token: String, refreshToken: String, login: String, id: UUID, expiresAt: Long)

object TokenInfo {
  implicit val tokenInfoFormat = Json.format[TokenInfo]
}

