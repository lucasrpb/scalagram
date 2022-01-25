package controllers

import models.{CodeInfo, Post, TokenInfo}
import models.slickmodels.{FeedTable, FollowerTable, PostTable, ProfileTable, UserTable}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Logging
import play.api.libs.Codecs.sha1
import play.api.libs.Files
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json}
import play.api.mvc.{AnyContentAsMultipartFormData, MultipartFormData}
import play.api.mvc.MultipartFormData.FilePart
import play.api.test._
import play.api.test.Helpers._
import slick.dbio.DBIO
import play.api.mvc.MultipartFormData._

import collection.JavaConverters._
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import repositories.MyPostgresProfile.api._

import java.nio.file.Paths

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class MainSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with Logging {

  "HomeController" should {

    val db = Database.forConfig("postgres")

    val actions = DBIO.seq(
      UserTable.users.schema.truncate,
      ProfileTable.profiles.schema.truncate,
      PostTable.posts.schema.truncate,
      FeedTable.feeds.schema.truncate,
      FollowerTable.followers.schema.truncate
    )

    Await.result(db.run(actions), Duration.Inf)
    db.close()

    val picturesFolder = Paths.get("public/pictures").toFile

    picturesFolder.listFiles().foreach(_.delete())

    // Login user0
    def login(user: CodeInfo, i: Int): String = {
      val request = FakeRequest(POST, "/users/login").withHeaders(
        "login" -> s"user${i}",
        "password" -> sha1("4321")
      )

      val users = route(app, request).get
      val sessionId = session(users).get("sessionId")

      assert(sessionId.isDefined)

      sessionId.get
    }

    def insertUsers(n: Int): Seq[CodeInfo] = {
      val request = FakeRequest(POST, "/users")
      var codes = Seq.empty[CodeInfo]

      for(i<-0 until n){
        val user = Json.obj(
          "username" -> JsString(s"user${i}"),
          "email" -> JsString(s"user${i}@gmail.com"),
          "phone" -> JsString(s"+55459981570${i}"),
          "password" -> JsString("4321")
        )

        val enrichedRequest = request.withHeaders(
          "Content-Type" -> "application/json"
        ).withBody(user)

        val users = route(app, enrichedRequest).get

        status(users) mustBe OK
        contentType(users) mustBe Some("application/json")

        val code = contentAsJson(users).validate[CodeInfo]

        assert(code.isSuccess)

        logger.info(s"confirmation code: ${code}")

        codes = codes :+ code.get
      }

      codes
    }

    def confirmUsers(codes: Seq[CodeInfo]): Unit = {
      for(c <- codes){
        val request = FakeRequest(PUT, s"/users/confirm/${c.code}")
        val users = route(app, request).get

        status(users) mustBe OK
        contentType(users) mustBe Some("application/json")

        val msg = (contentAsJson(users) \\ "status").headOption

        assert(msg.isDefined && msg.value.as[String].contains("User confirmed successfully!"))
      }
    }

    def followUsers(n: Int, sessionId: String, followers: Seq[CodeInfo]): Unit = {
      for(i<-1 until n){
       val c = followers(i)

       val request = FakeRequest(POST, "/feed/follow").withSession(
         "sessionId" -> sessionId
       ).withBody(Json.obj(
         "followerId" -> JsString(c.id.toString),
       ))

        val users = route(app, request).get

        status(users) mustBe OK
        contentType(users) mustBe Some("application/json")

        val msg = (contentAsJson(users) \\ "status").headOption

        assert(msg.isDefined && msg.value.as[String].contains("You successfully followed this user!"))
      }
    }

    def postContent(sessionId: String): Seq[Post] = {

      val file = Paths.get("images").toFile

      assert(file.isDirectory)

      val images = file.listFiles().toList

      for(img <- images){
        val tf = SingletonTemporaryFileCreator.create(img.toPath)
        val part = FilePart[TemporaryFile](key = "img", filename = img.getName, contentType = Some("application/image"), ref = tf)

        val formData = MultipartFormData(dataParts = Map(
          "data" -> Seq(new String(Json.toBytes(Json.obj(
            "description" -> "Best picture!",
            "tags" -> JsArray(
              Seq(JsString("pic"))
            )
          ))))
        ), files = Seq(part), badParts = Seq())

        val request = FakeRequest(POST, "/posts/upload").withSession(
          "sessionId" -> sessionId
        ).withMultipartFormDataBody(formData)
        val posts = route(app, request).get

        println(contentAsJson(posts).value)

        status(posts) mustBe OK
      }

      Seq.empty[Post]
    }

    "insert a pool of users, confirming them, creating following relations, posting content" in {

      val n = 10

      // Creating users
      val codes = insertUsers(n)

      // Confirming users...
      confirmUsers(codes)

      // Login user0
      val u0Session = login(codes(0), 0)

      // Set user0 following all other users
      followUsers(n, u0Session, codes)

      // Login user1
      val u1Session = login(codes(1), 1)

      postContent(u1Session)

    }

  }
}
