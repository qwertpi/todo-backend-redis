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
        jedis.hset(s"todos:$uid", todo.toRedisToDo.toMap.asJava)
        todo.asJson

    def getAllToDos(): Json =
        val list =
            for uid <- jedis.smembers("uids").asScala
            yield jedis
                .hgetAll(s"todos:$uid")
                .asScala
                .toMap + ("uid" -> uid) + ("url" -> s"/$uid")
        list.map(h => RedisToDo(h("uid"), h("title"), h("completed")).toAPIToDo)
            .asJson

    def delAllToDos(): Unit =
        val to_delete: Seq[String] = jedis
            .smembers("uids")
            .asScala
            .map("todos:" + _)
            .toSeq :+ "uids" :+ "next-uid"
        jedis.del(to_delete*)
