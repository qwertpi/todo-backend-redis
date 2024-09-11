import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Hostname, Port}
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.Request
import org.http4s.server.middleware.{CORS, ErrorAction}

object Main extends IOApp:
    def logRequest(req: Request[IO]): IO[Unit]               =
        IO.println(s"Request: ${req.method} ${req.uri}")
            *>
                req.bodyText.compile.string.flatMap(body =>
                    IO.println(s"Body: $body"))
    def logErrorMessage(err: Throwable): IO[Unit]            =
        IO.println(s"Error: ${err.getMessage()}")
    def logError(req: Request[IO], err: Throwable): IO[Unit] =
        logRequest(req) *> logErrorMessage(err)

    override def run(args: List[String]): IO[ExitCode] =
        val db      = if args.contains("--test") then 15 else 0
        val httpApp = CORS.policy.withAllowOriginAll(
            ErrorAction.httpRoutes[IO](Routes.routes(db), logError).orNotFound)

        val server = for {
            _ <-
                EmberServerBuilder
                    .default[IO]
                    .withHost(Hostname.fromString(Constants.host).get)
                    .withPort(Port.fromInt(Constants.port).get)
                    .withHttpApp(httpApp)
                    .build
        } yield ()
        server.useForever
