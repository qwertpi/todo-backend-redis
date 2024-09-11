case class NewToDo(title: String)
case class ToDo(uid: Long, title: String, completed: Boolean)

extension (newToDo: NewToDo)
    def toToDo(uid: Long): ToDo = ToDo(uid, newToDo.title, false)

extension (todo: ToDo)
    def toRedisHash() = Map.from(
        todo.productElementNames.zip(
            todo.productIterator.map(_.toString()))) - "uid"

extension (map: Map[String, String])
    def toToDo(): ToDo =
        ToDo(map("uid").toLong, map("title"), map("completed").toBoolean)
