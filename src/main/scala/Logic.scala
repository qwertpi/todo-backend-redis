import java.util.UUID.randomUUID
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.*
import redis.clients.jedis.Jedis
import scala.jdk.CollectionConverters.*

class Logic(val db: Int):
    val jedis = Jedis()
    jedis.select(db)

    def redisPing(msg: String): String = msg match
        case "" => jedis.ping()
        case _  => jedis.ping(msg)

    @annotation.tailrec
    private def generateUID(): String =
        val uid = randomUUID().toString()
        if jedis.sadd("uids", uid) != 1 then generateUID() else uid

    def addToDo(newTodo: NewToDo): Json =
        val uid = generateUID()
        jedis.hset(s"todos:$uid", newTodo.toToDo(uid).toRedisToDo.toMap.asJava)
        getToDo(uid)

    private def getToDoFromRedis(uid: String): RedisToDo =
        val map = jedis.hgetAll(s"todos:$uid").asScala.toMap
        RedisToDo(uid, map("title"), map("completed"))

    def getToDo(uid: String): Json =
        getToDoFromRedis(uid).toAPIToDo.asJson

    def getToDo(uid: Long): Json = getToDo(uid.toString())

    def getAllToDos(): Json =
        jedis.smembers("uids").asScala.map(getToDoFromRedis(_).toAPIToDo).asJson

    def delAllToDos(): Unit =
        val to_delete: Seq[String] = jedis
            .smembers("uids")
            .asScala
            .map("todos:" + _)
            .toSeq :+ "uids" :+ "next-uid"
        jedis.del(to_delete*)
