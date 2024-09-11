import org.http4s.implicits.uri
import org.http4s.Uri

case class NewToDo(title: String)
case class ToDo(uid: Long, title: String, completed: Boolean)
case class RedisToDo(uid: String, title: String, completed: String)
case class APIToDo(uid: Long, title: String, completed: Boolean, url: String)

extension [T <: Product](c: T)
    def toMap[V]: Map[String, V] =
        val keys   = c.productElementNames
        val values = c.productIterator
        Map.from(keys.zip(values.map(_.asInstanceOf[V])))

extension (newToDo: NewToDo)
    def toToDo(uid: Long): ToDo = ToDo(uid, newToDo.title, false)

extension (todo: ToDo)
    def toRedisToDo: RedisToDo =
        RedisToDo(todo.uid.toString(), todo.title, todo.completed.toString())

extension (redisToDo: RedisToDo)
    def toAPIToDo: APIToDo = APIToDo(
        redisToDo.uid.toLong,
        redisToDo.title,
        redisToDo.completed.toBoolean,
        s"${Constants.root}/${redisToDo.uid}")
