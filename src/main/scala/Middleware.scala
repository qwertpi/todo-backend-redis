import annotation.tailrec
import java.util.UUID.randomUUID
import io.circe.Json
import io.circe.generic.auto.deriveEncoder
import io.circe.syntax.EncoderOps
import org.http4s.circe.{encodeUri, jsonEncoder}
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

    def redisPing(msg: Option[String]): HTTPResponse = Ok(
        msg match
            case None      => useJedis(jedis => jedis.ping())
            case Some(msg) => useJedis(jedis => jedis.ping(msg)),
    )

    @tailrec
    private def generateUID(): String =
        val uid = randomUUID().toString()
        /* For security we track all UIDs ever issued to avoid reissuing,
           this prevents accidental leaking of other user's data if a user
           accidentally navigates back to a link to a todo they deleted */
        if useJedis(jedis => jedis.sadd("uids", uid)) != 1 then generateUID()
        else
            useJedis(jedis => jedis.sadd("activeUIDs", uid))
            uid

    def addToDo(newTodo: NewToDo): HTTPResponse =
        val uid = generateUID()
        useJedis(jedis =>
            jedis.hset(
                s"todos:$uid",
                newTodo.toToDo(uid).toRedisToDo.toMap.asJava))
        getToDo(uid)

    def delToDo(uid: String): HTTPResponse =
        useJedis(jedis =>
            jedis.del(s"todos:$uid")
            jedis.srem("activeUIDs", uid),
        )
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
            useJedis(jedis => jedis.smembers("activeUIDs")).asScala
                .map(getToDoFromRedis(_).get.toAPIToDo)
                .asJson)

    def delAllToDos(): HTTPResponse =
        val to_delete: Seq[String] =
            useJedis(jedis => jedis.smembers("activeUIDs")).asScala
                .map("todos:" + _)
                .toSeq :+ "activeUIDs"
        useJedis(jedis => jedis.del(to_delete*))
        Ok()
