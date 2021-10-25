package models

import play.api.libs.Codecs.sha1
import play.api.libs.json.{Format, Json}

import java.util.UUID

case class User(id: UUID = UUID.randomUUID(),
                username: String,
                var password: String,
                email: String,
                phone: String,
                code: String = UUID.randomUUID.toString,
                token: String = UUID.randomUUID.toString,
                refreshToken: String = UUID.randomUUID.toString,
                createdAt: Long = System.currentTimeMillis(),
                tokenLastUpdate: Long = System.currentTimeMillis(),
                codeLastUpdate: Long = System.currentTimeMillis(),
                status: Int = UserStatus.NOT_CONFIRMED) {

  this.password = sha1(password)

}

object User {
  implicit val userFormat = Jsonc
}
