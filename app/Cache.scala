package app

import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RedisCache])
trait Cache {
  def put(key: String, value: Array[Byte]): Boolean
  def get(key: String): Option[Array[Byte]]
}

@Singleton
class RedisCache @Inject() (implicit val lifecycle: ApplicationLifecycle,
                            val ec: ExecutionContext) extends Cache with Logging {

  logger.info(s"instance: ${this.hashCode()}")

  val jedisConfig = new JedisPoolConfig()
  jedisConfig.setMaxIdle(0)

  protected val pool = new JedisPool(jedisConfig, "127.0.0.1")

  lifecycle.addStopHook { () =>
    val client = pool.getResource

    Future.successful {
      if(client.isConnected){
        pool.close()
      }
    }
  }

  override def put(key: String, value: Array[Byte]): Boolean = {
    val client = pool.getResource
    client.set(key.getBytes(), value)
    client.close()
    true
  }

  override def get(key: String): Option[Array[Byte]] = {
    val client = pool.getResource

    val response = client.get(key.getBytes())

    client.close()

    response match {
      case null => None
      case data => Some(data)
    }
  }

}