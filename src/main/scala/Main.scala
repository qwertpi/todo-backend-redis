import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Hostname, Port}
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, ErrorAction}
import redis.clients.jedis.JedisPool

object Main extends IOApp:
    override def run(args: List[String]): IO[ExitCode] =
        val db        = if args.contains("--test") then 15 else 0
        val jedisPool = JedisPool()

        val httpApp = CORS.policy.withAllowOriginAll(
            ErrorAction
                .httpRoutes[IO](
                    Routes.routes(jedisPool, db),
                    (_, e) =>
                        IO(System.err.println(s"Error: ${e.getMessage()}")))
                .orNotFound)
        val server  = for {
            _ <-
                EmberServerBuilder
                    .default[IO]
                    .withHost(Hostname.fromString(Constants.host).get)
                    .withPort(Port.fromInt(Constants.port).get)
                    .withHttpApp(httpApp)
                    .build
        } yield ()
        server.useForever
