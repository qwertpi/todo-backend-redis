import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import redis.clients.jedis.Jedis
import scala.jdk.CollectionConverters.*

class Logic(val db: Int):
    val jedis = Jedis()
    jedis.select(db)

    def redisPing(msg: String): String = msg match
        case "" => jedis.ping()
        case _  => jedis.ping(msg)

    def addToDo(newTodo: NewToDo): Json =
        val uid        = jedis.incr("next-uid")
        val todo: ToDo = newTodo.toToDo(uid)
        val ret        = jedis.sadd("uids", uid.toString())
        if ret != 1 then
            throw RuntimeException("Redis error: UID already exists")
        jedis.hset(s"todos:$uid", todo.toRedisToDo.toMap.asJava)
        getToDo(uid)

    private def getToDoFromRedis(uid: String): RedisToDo =
        val map = jedis.hgetAll(s"todos:$uid").asScala.toMap
        RedisToDo(uid, map("title"), map("completed"))

    def getToDo(uid: String): Json =
        getToDoFromRedis(uid).toAPIToDo.asJson
    def getToDo(uid: Long): Json   = getToDo(uid.toString())

    def getAllToDos(): Json =
        jedis.smembers("uids").asScala.map(getToDoFromRedis(_).toAPIToDo).asJson

    def delAllToDos(): Unit =
        val to_delete: Seq[String] = jedis
            .smembers("uids")
            .asScala
            .map("todos:" + _)
            .toSeq :+ "uids" :+ "next-uid"
        jedis.del(to_delete*)
