import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import redis.clients.jedis.Jedis
import scala.jdk.CollectionConverters.*

object Logic:
    val jedis = Jedis()

    def redisPing(msg: String): String = msg match
        case "" => jedis.ping()
        case _  => jedis.ping(msg)

    def addToDo(newTodo: NewToDo): Json =
        val uid        = jedis.incr("next-uid")
        val todo: ToDo = newTodo.toToDo(uid)
        val ret        = jedis.sadd("uids", uid.toString())
        if ret != 1 then
            throw RuntimeException("Redis error: UID already exists")
        jedis.hset(s"todos:$uid", todo.toRedisHash().asJava)
        todo.asJson

    def getAllToDos(): Json =
        val list =
            for uid <- jedis.smembers("uids").asScala
            yield jedis.hgetAll(s"todos:$uid").asScala.toMap + ("uid" -> uid)
        list.map(_.toToDo()).asJson

    def delAllToDos(): Unit =
        jedis.del(jedis.smembers("uids").asScala.map("todos:" + _).toSeq*)
        jedis.del("uids")
        jedis.del("next-uid")
