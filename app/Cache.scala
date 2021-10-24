package app

import com.google.inject.ImplementedBy
import play.api.inject.ApplicationLifecycle
import redis.clients.jedis.Jedis

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RedisCache])
trait Cache {
  def put(key: String, value: Array[Byte]): Boolean
  def get(key: String): Option[Array[Byte]]
}

@Singleton
class RedisCache @Inject() (implicit val lifecycle: ApplicationLifecycle,
                            val ec: ExecutionContext) extends Cache {

  protected val client = new Jedis("127.0.0.1", 6379)

  lifecycle.addStopHook { () =>
    Future.successful(client.close())
  }

  override def put(key: String, value: Array[Byte]): Boolean = {
    client.set(key.getBytes(), value)
    true
  }

  override def get(key: String): Option[Array[Byte]] = {
    client.get(key.getBytes()) match {
      case null => None
      case data => Some(data)
    }
  }

}