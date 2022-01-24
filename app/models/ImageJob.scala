package models

import play.api.libs.json.Json
import java.util.UUID

case class ImageJob(id: UUID,
                    ext: String,
                    filePath: String,
                    topic: String
                   )

object ImageJob {
  implicit val imageTaskFormat = Json.using[Json.WithDefaultValues].format[ImageJob]
}




