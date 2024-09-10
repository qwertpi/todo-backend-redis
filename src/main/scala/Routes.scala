import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.Status
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.implicits.uri

object Routes:
	val routes = HttpRoutes.of[IO] {
		case DELETE -> Root => Status.Ok(Logic.delAllToDos())
		case PUT -> Root => Status.Ok(Logic.addToDo())
		case GET -> Root => Status.Ok(Logic.getAllToDos())
		case GET -> Root / "ping" => Status.PermanentRedirect(Location(uri"/ping/"))
		case GET -> Root / "ping" / msg   => Status.Ok(Logic.redisPing(msg))
	}