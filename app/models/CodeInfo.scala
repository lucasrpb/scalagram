package models

import play.api.libs.json.Json

case class CodeInfo(code: String, lastUpdate: Long, status: Option[Int] = None)

object CodeInfo {
  implicit val codeInfoFormat = Json.using[Json.WithDefaultValues].format[CodeInfo]
}



