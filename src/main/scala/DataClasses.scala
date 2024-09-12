import io.circe.Json
import org.http4s.Uri

// What clients POST to create todos
case class NewToDo(title: String, order: Option[Int])
// How todos are represented in our Scala code
case class ToDo(
    uid: String,
    title: String,
    order: Option[Int],
    completed: Boolean)
// How todos are represented in Redis
case class RedisToDo(
    uid: String,
    title: String,
    order: String,
    completed: String)
// How todos are represented in the API GET responses
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

// Allow a case class to be converted to a Map
extension [T <: Product](c: T)
    def toMap[V]: Map[String, V] =
        val keys   = c.productElementNames
        val values = c.productIterator
        Map.from(keys.zip(values.map(_.asInstanceOf[V])))

/* We have a pipeline whereby a newToDo becomes a ToDo
   which becomes a RedisToDo which repeatedly becomes an APIToDo */
extension (newToDo: NewToDo)
    def toToDo(uid: String): ToDo =
        ToDo(uid, newToDo.title, newToDo.order, false)

extension (todo: ToDo)
    def toRedisToDo: RedisToDo =
        RedisToDo(
            todo.uid,
            todo.title,
            /* The default String here is arbitrary with the condition
               that it can't be parsed as a number later */
            todo.order.map(_.toString()).getOrElse("null"),
            todo.completed.toString(),
        )

extension (redisToDo: RedisToDo)
    def toAPIToDo: APIToDo =
        APIToDo(
            redisToDo.uid,
            redisToDo.title,
            redisToDo.order.toIntOption,
            redisToDo.completed.toBoolean,
            Uri.fromString(s"${Constants.root}/${redisToDo.uid}").toOption.get,
        )
