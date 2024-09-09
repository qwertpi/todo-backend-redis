import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{ipv4, port}
import fs2.io.net.Network
import org.http4s.HttpRoutes
import org.http4s.Status
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

object Logic:
	def sayHello(name: String): String = "Hi there " + name

object Routes:
	val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
		case GET -> Root / "hello" / name => Status.Ok(Logic.sayHello(name))
	}

object Main extends IOApp.Simple:
	// This is where we will put CORS
	val httpApp = Logger.httpApp(true, true)(Routes.routes.orNotFound)

	val server = for {
		_ <-
			EmberServerBuilder.default[IO]
				.withHost(ipv4"0.0.0.0")
				.withPort(port"8080")
				.withHttpApp(httpApp)
				.build
	} yield ()
	val run = server.useForever
