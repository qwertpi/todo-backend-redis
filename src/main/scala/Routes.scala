import cats.effect.IO
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.Status
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.headers.Location
import org.http4s.implicits.uri

object Routes:
    val routes = HttpRoutes.of[IO] {
        case DELETE -> Root         => Status.Ok(Logic.delAllToDos())
        case request @ POST -> Root =>
            for
                todo <- request.as[NewToDo]
                resp <- Ok(Logic.addToDo(todo))
            yield resp
        case GET -> Root            => Ok(Logic.getAllToDos())
        case GET -> Root / "ping"   => PermanentRedirect(Location(uri"/ping/"))
        case GET -> Root / "ping" / msg => Ok(Logic.redisPing(msg))
        case GET -> Root / uid          => Ok(Logic.getToDo(uid))
    }
