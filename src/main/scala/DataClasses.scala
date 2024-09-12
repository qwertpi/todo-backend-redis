import io.circe.Json
import org.http4s.Uri

case class NewToDo(title: String, order: Option[Int])
case class ToDo(
    uid: String,
    title: String,
    order: Option[Int],
    completed: Boolean)
case class RedisToDo(
    uid: String,
    title: String,
    order: String,
    completed: String)
case class APIToDo(
    uid: String,
    title: String,
    order: Option[Int],
    completed: Boolean,
    url: Uri)

extension (json: Json)
    // toString keeps the quotes around string objects which is undesirable
    def toStringRobust = json.asString match
        case None    => json.toString()
        case Some(s) => s

extension [T <: Product](c: T)
    def toMap[V]: Map[String, V] =
        val keys   = c.productElementNames
        val values = c.productIterator
        Map.from(keys.zip(values.map(_.asInstanceOf[V])))

extension (newToDo: NewToDo)
    def toToDo(uid: String): ToDo =
        ToDo(uid, newToDo.title, newToDo.order, false)

extension (todo: ToDo)
    def toRedisToDo: RedisToDo =
        RedisToDo(
            todo.uid,
            todo.title,
            todo.order.map(_.toString()).getOrElse("null"),
            todo.completed.toString())

extension (redisToDo: RedisToDo)
    def toAPIToDo: APIToDo =
        APIToDo(
            redisToDo.uid,
            redisToDo.title,
            redisToDo.order.toIntOption,
            redisToDo.completed.toBoolean,
            Uri.fromString(s"${Constants.root}/${redisToDo.uid}").toOption.get,
        )
