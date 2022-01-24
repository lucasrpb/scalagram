package models

import play.api.libs.json.{Json, Reads, Writes}

import java.util.UUID

case class UpdatePost(id: UUID,
                      description: String,
                      var tags: List[String] = List.empty[String]){
  tags = tags.map(_.toLowerCase.trim)
}

object UpdatePost {
  implicit val upFormat = Json.using[Json.WithDefaultValues].format[UpdatePost]
  implicit val upSeqReads = Reads.seq(upFormat)
  implicit val upSeqWrites = Writes.seq(upFormat)
}
