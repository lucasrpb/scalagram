package models

import play.api.libs.json.{Json, Reads, Writes}

import java.util.UUID

case class Post(id: UUID = UUID.randomUUID,
                userId: UUID,
                imgType: String = "",
                description: Option[String] = Some(""),
                var tags: List[String] = List.empty[String],
                postedAt: Long = System.currentTimeMillis()){
  tags = tags.map(_.toLowerCase.trim)
}

object Post {
  implicit val postFormat = Json.using[Json.WithDefaultValues].format[Post]
  implicit val postSeqReads = Reads.seq(postFormat)
  implicit val postSeqWrites = Writes.seq(postFormat)
}