import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.Status
import org.http4s.circe._
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.implicits.uri

object Routes:
	val routes = HttpRoutes.of[IO]{
		case DELETE -> Root => Status.Ok(Logic.delAllToDos())
		case request @ POST -> Root => for
			todo <- request.as[NewToDo]
			_ = Logic.addToDo(todo)
			resp <- Ok(todo.asJson)
		yield resp
		case GET -> Root => Ok(Logic.getAllToDos())
		case GET -> Root / "ping" => PermanentRedirect(Location(uri"/ping/"))
		case GET -> Root / "ping" / msg   => Ok(Logic.redisPing(msg))
	}