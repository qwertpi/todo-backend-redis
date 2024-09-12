import java.util.UUID.randomUUID
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import redis.clients.jedis.{Jedis, JedisPool}
import scala.jdk.CollectionConverters.*

class Logic(jedisPool: JedisPool, db: Int):
    private def useJedis[T](func: Jedis => T): T =
        val jedis = jedisPool.getResource()
        jedis.select(db)
        val ret   = func(jedis)
        jedis.close()
        ret

    def redisPing(msg: String): String =
        msg match
            case "" => useJedis(jedis => jedis.ping())
            case _  => useJedis(jedis => jedis.ping(msg))

    @annotation.tailrec
    private def generateUID(): String =
        val uid = randomUUID().toString()
        if useJedis(jedis => jedis.sadd("uids", uid)) != 1 then generateUID()
        else uid

    def addToDo(newTodo: NewToDo): Json =
        val uid = generateUID()
        useJedis(jedis =>
            jedis.hset(
                s"todos:$uid",
                newTodo.toToDo(uid).toRedisToDo.toMap.asJava))
        getToDo(uid)

    private def getToDoFromRedis(uid: String): RedisToDo =
        val map = useJedis(jedis => jedis.hgetAll(s"todos:$uid")).asScala.toMap
        RedisToDo(uid, map("title"), map("order"), map("completed"))

    def getToDo(uid: String): Json =
        getToDoFromRedis(uid).toAPIToDo.asJson

    def updateToDo(uid: String, patch: Map[String, Json]): Json =
        useJedis(jedis =>
            jedis.hset(
                s"todos:$uid",
                patch.map((k, v) => k -> v.toStringRobust).asJava))
        getToDo(uid)

    def getAllToDos(): Json =
        useJedis(jedis => jedis.smembers("uids")).asScala
            .map(getToDoFromRedis(_).toAPIToDo)
            .asJson

    def delAllToDos(): Unit =
        val to_delete: Seq[String] =
            useJedis(jedis => jedis.smembers("uids")).asScala
                .map("todos:" + _)
                .toSeq :+ "uids" :+ "next-uid"
        useJedis(jedis => jedis.del(to_delete*))
        ()
