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
import io.circe.Json
import redis.clients.jedis.JedisPool

object Routes:
    def routes(jedisPool: JedisPool, db: Int) =
        val logic = Logic(jedisPool, db)
        HttpRoutes.of[IO] {
            case DELETE -> Root                => Status.Ok(logic.delAllToDos())
            case request @ POST -> Root        =>
                for
                    todo <- request.as[NewToDo]
                    resp <- Ok(logic.addToDo(todo))
                yield resp
            case GET -> Root                   => Ok(logic.getAllToDos())
            case GET -> Root / "ping"          =>
                PermanentRedirect(Location(uri"/ping/"))
            case GET -> Root / "ping" / msg    => Ok(logic.redisPing(msg))
            case GET -> Root / uid             => Ok(logic.getToDo(uid))
            case request @ PATCH -> Root / uid =>
                for
                    // Json is the equivalent of Any for circe
                    patch <- request.as[Map[String, Json]]
                    resp  <- Ok(logic.updateToDo(uid, patch))
                yield resp
        }
