package helpers

import com.lambdaworks.redis.codec.{ByteArrayCodec, RedisCodec}
import com.lambdaworks.redis.{RedisClient, RedisConnection, RedisURI}
import play.api.Logging
import redis.clients.jedis.{JedisPool, JedisPoolConfig}

object RedisTest extends Logging {

  def main(args: Array[String]): Unit = {

    /*val jedisConfig = new JedisPoolConfig()
    jedisConfig.setMaxIdle(0)

    val pool = new JedisPool(jedisConfig, "redis://bxk2u26SKdBWto41e3qlRBoPeOe3ApfK@redis-10214.c1.us-east1-2.gce.cloud.redislabs.com:10214")

    val redis = pool.getResource()

    redis.set("hello", "world")

    logger.debug(s"value: ${redis.get("hello")}")

    pool.close()*/

    val redisClient = RedisClient.create("redis://bxk2u26SKdBWto41e3qlRBoPeOe3ApfK@redis-10214.c1.us-east1-2.gce.cloud.redislabs.com:10214")
    val connection = redisClient.connect(new ByteArrayCodec()).sync()

    connection.set("hello".getBytes(), "world".getBytes())

    System.out.println(s"Connected to Redis: ${new String(connection.get("hello".getBytes()))}")

    connection.close()
    redisClient.shutdown()
  }

}
