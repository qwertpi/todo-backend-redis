import redis.clients.jedis.Jedis

object Logic:
	val jedis = Jedis()

	def redisPing(msg: String): String = msg match
		case "" => jedis.ping()
		case  _ => jedis.ping(msg)

	def addToDo(): Unit = ???

	def getAllToDos(): String = ???

	def delAllToDos(): Unit = ???