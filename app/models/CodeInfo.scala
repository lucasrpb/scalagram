package models

import play.api.libs.json.Json
import java.util.UUID

case class CodeInfo(id: UUID, code: String, lastUpdate: Long, status: Option[Int] = None)

object CodeInfo {
  implicit val codeInfoFormat = Json.using[Json.WithDefaultValues].format[CodeInfo]
}



