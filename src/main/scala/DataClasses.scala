case class NewToDo(title: String)
case class ToDo(uid: Int, title: String, completed: Boolean)

extension (newToDo: NewToDo)
  def create(uid: Int): ToDo = ToDo(uid, newToDo.title, false)