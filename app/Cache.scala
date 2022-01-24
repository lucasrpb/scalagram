package app

import com.google.common.base.Charsets
import com.google.inject.ImplementedBy
import com.lambdaworks.redis.RedisClient
import com.lambdaworks.redis.codec.ByteArrayCodec
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.compat.java8.FutureConverters._

@ImplementedBy(classOf[RedisCache])
trait Cache {
  def put(key: String, value: Array[Byte]): Future[Boolean]
  def get(key: String): Future[Option[Array[Byte]]]
}

@Singleton
class RedisCache @Inject() (implicit val lifecycle: ApplicationLifecycle,
                            val ec: ExecutionContext) extends Cache with Logging {

  logger.info(s"instance: ${this.hashCode()}")

  val redisClient = RedisClient.create("redis://bxk2u26SKdBWto41e3qlRBoPeOe3ApfK@redis-10214.c1.us-east1-2.gce.cloud.redislabs.com:10214")
  val connection = redisClient.connect(new ByteArrayCodec()).async()

  override def put(key: String, value: Array[Byte]): Future[Boolean] = {
    connection.set(key.getBytes(Charsets.UTF_8), value).toScala.map(_ != null)
  }

  override def get(key: String): Future[Option[Array[Byte]]] = {
    connection.get(key.getBytes(Charsets.UTF_8)).toScala.map(bytes => if(bytes == null) None else Some(bytes))
  }

  lifecycle.addStopHook { () =>
    for {
       _ <- connection.flushall().toScala
       _ <- Future.successful(connection.close())
    } yield {}
  }
}