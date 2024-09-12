import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.headers.Location
import org.http4s.implicits.uri
import io.circe.Json
import redis.clients.jedis.JedisPool

object Routes:
    def routes(jedisPool: JedisPool, db: Int) =
        val middleware = Middleware(jedisPool, db)
        HttpRoutes.of[IO] {
            case DELETE -> Root                => middleware.delAllToDos()
            case request @ POST -> Root        =>
                for
                    todo <- request.as[NewToDo]
                    resp <- middleware.addToDo(todo)
                yield resp
            case GET -> Root                   => middleware.getAllToDos()
            case GET -> Root / "ping"          =>
                PermanentRedirect(Location(uri"/ping/"))
            case GET -> Root / "ping" / msg    => middleware.redisPing(msg)
            case DELETE -> Root / uid          => middleware.delToDo(uid)
            case GET -> Root / uid             => middleware.getToDo(uid)
            case request @ PATCH -> Root / uid =>
                for
                    // Json is the equivalent of Any for circe
                    patch <- request.as[Map[String, Json]]
                    resp  <- middleware.updateToDo(uid, patch)
                yield resp
        }
