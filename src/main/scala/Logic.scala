import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import redis.clients.jedis.Jedis

object Logic:
    val jedis                = Jedis()
    var nextUid              = 1
    var database: List[ToDo] = List()

    def redisPing(msg: String): String = msg match
        case "" => jedis.ping()
        case _  => jedis.ping(msg)

    def addToDo(todo: NewToDo): Unit =
        database = database ++ List(todo.create(nextUid))
        nextUid += 1

    def getAllToDos(): Json = database.asJson

    def delAllToDos(): Unit = database = List()
