import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.Status
import org.http4s.dsl.io._

object Routes:
	val routes = HttpRoutes.of[IO] {
		case GET -> Root => Status.Ok("Site is running :)")
		case GET -> Root / "ping" => Status.Ok(Logic.redisPing(""))
		case GET -> Root / "ping" / msg   => Status.Ok(Logic.redisPing(msg))
	}