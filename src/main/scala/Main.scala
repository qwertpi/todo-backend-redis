import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{ipv4, port}
import fs2.io.net.Network
import org.http4s.HttpRoutes
import org.http4s.Status
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import redis.clients.jedis.Jedis

object Logic:
	val jedis = Jedis()
	def redisPing(msg: String): String = msg match
		case "" => jedis.ping()
		case  _ => jedis.ping(msg)

object Routes:
	val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
		case GET -> Root => Status.Ok("Site is running :)")
		case GET -> Root / "ping" => Status.Ok(Logic.redisPing(""))
		case GET -> Root / "ping" / msg   => Status.Ok(Logic.redisPing(msg))
	}

object Main extends IOApp.Simple:
	val httpApp = CORS.policy.withAllowOriginAll(
		Logger.httpApp(true, true)(
			Routes.routes.orNotFound))

	val server = for {
		_ <-
			EmberServerBuilder.default[IO]
				.withHost(ipv4"127.0.0.1")
				.withPort(port"8080")
				.withHttpApp(httpApp)
				.build
	} yield ()
	val run = server.useForever
