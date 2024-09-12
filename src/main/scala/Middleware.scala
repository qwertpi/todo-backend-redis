import java.util.UUID.randomUUID
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import org.http4s.Response
import org.http4s.dsl.io.*
import redis.clients.jedis.{Jedis, JedisPool}
import scala.jdk.CollectionConverters.*
import cats.effect.IO

type HTTPResponse = IO[Response[IO]]

class Middleware(jedisPool: JedisPool, db: Int):
    private def useJedis[T](func: Jedis => T): T =
        val jedis = jedisPool.getResource()
        jedis.select(db)
        val ret   = func(jedis)
        jedis.close()
        ret

    def redisPing(msg: String): HTTPResponse = Ok(
        msg match
            case "" => useJedis(jedis => jedis.ping())
            case _  => useJedis(jedis => jedis.ping(msg)),
    )

    @annotation.tailrec
    private def generateUID(): String =
        val uid = randomUUID().toString()
        if useJedis(jedis => jedis.sadd("uids", uid)) != 1 then generateUID()
        else uid

    def addToDo(newTodo: NewToDo): HTTPResponse =
        val uid = generateUID()
        useJedis(jedis =>
            jedis.hset(
                s"todos:$uid",
                newTodo.toToDo(uid).toRedisToDo.toMap.asJava))
        getToDo(uid)

    def delToDo(uid: String): HTTPResponse =
        useJedis(jedis => jedis.del(s"todos:$uid"))
        Ok()

    private def getToDoFromRedis(uid: String): Option[RedisToDo] =
        val map = useJedis(jedis => jedis.hgetAll(s"todos:$uid")).asScala.toMap
        Option.when(map.nonEmpty)(
            RedisToDo(uid, map("title"), map("order"), map("completed")))

    def getToDo(uid: String): HTTPResponse = getToDoFromRedis(uid) match
        case None    => NotFound()
        case Some(t) => Ok(t.toAPIToDo.asJson)

    def updateToDo(uid: String, patch: Map[String, Json]): HTTPResponse =
        useJedis(jedis =>
            jedis.hset(
                s"todos:$uid",
                patch.map((k, v) => k -> v.toStringRobust).asJava))
        getToDo(uid)

    def getAllToDos(): HTTPResponse =
        Ok(
            useJedis(jedis => jedis.smembers("uids")).asScala
                .map(getToDoFromRedis(_).get.toAPIToDo)
                .asJson)

    def delAllToDos(): HTTPResponse =
        val to_delete: Seq[String] =
            useJedis(jedis => jedis.smembers("uids")).asScala
                .map("todos:" + _)
                .toSeq :+ "uids"
        useJedis(jedis => jedis.del(to_delete*))
        Ok()
