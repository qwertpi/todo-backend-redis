import io.circe.Json
import org.http4s.Uri

case class NewToDo(title: String)
case class ToDo(uid: String, title: String, completed: Boolean)
case class RedisToDo(uid: String, title: String, completed: String)
case class APIToDo(uid: String, title: String, completed: Boolean, url: Uri)

extension (json: Json)
    // json.toString keeps the quotation marks around string objects
    def toStringRobust = json.asString match
        case None    => json.toString()
        case Some(s) => s

extension [T <: Product](c: T)
    def toMap[V]: Map[String, V] =
        val keys   = c.productElementNames
        val values = c.productIterator
        Map.from(keys.zip(values.map(_.asInstanceOf[V])))

extension (newToDo: NewToDo)
    def toToDo(uid: String): ToDo = ToDo(uid, newToDo.title, false)

extension (todo: ToDo)
    def toRedisToDo: RedisToDo =
        RedisToDo(todo.uid, todo.title, todo.completed.toString())

extension (redisToDo: RedisToDo)
    def toAPIToDo: APIToDo =
        APIToDo(
            redisToDo.uid,
            redisToDo.title,
            redisToDo.completed.toBoolean,
            Uri.fromString(s"${Constants.root}/${redisToDo.uid}").right.get)
