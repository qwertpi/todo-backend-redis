import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import redis.clients.jedis.Jedis

object Logic:
	val jedis = Jedis()
	var database = List(ToDo(1, "Foo", false), ToDo(2, "bar", true))

	def redisPing(msg: String): String = msg match
		case "" => jedis.ping()
		case  _ => jedis.ping(msg)

	def addToDo(todo: Json): Unit = ???

	def getAllToDos(): Json = database.asJson

	def delAllToDos(): Unit = database = List()