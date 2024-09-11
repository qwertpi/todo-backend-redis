import munit.Assertions.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import sttp.client3.{basicRequest, SimpleHttpClient, UriContext}
import sttp.client3.Response

def getBody[W, T](response: Response[Either[W, T]]): T =
    response.body match
        case Left(err)   => fail(s"HTTP error: ${err.toString()}")
        case Right(body) => body

def assertResponseBodyIs[W, T](response: Response[Either[W, T]], target: T) =
    response.body match
        case Left(err)   => fail(s"HTTP error: ${err.toString()}")
        case Right(body) => assertEquals(body, target)

class MySuite extends munit.FunSuite:
    val root   = "http://localhost:8080"
    val client = SimpleHttpClient()

    test("server is up"):
        val response = client.send(basicRequest.get(uri"$root"))
        assert(response.isSuccess)

    test("redis is up"):
        val response = client.send(basicRequest.get(uri"$root/ping"))
        assertResponseBodyIs(response, "PONG")

    test("post gives added"):
        val postResponse1Body = getBody(
            client.send(
                basicRequest.post(uri"$root").body("{\"title\": \"Foo\"}")))

        decode[ToDo](postResponse1Body) match
            case Left(err)   => fail(err.toString())
            case Right(todo) =>
                assertEquals(todo.title, "Foo")
                assertEquals(todo.completed, false)

        val postResponse2Body = getBody(
            client.send(
                basicRequest.post(uri"$root").body("{\"title\": \"Bar\"}")))

        decode[ToDo](postResponse2Body) match
            case Left(err)   => fail(err.toString())
            case Right(todo) =>
                assertEquals(todo.title, "Bar")
                assertEquals(todo.completed, false)

        val getResponseBody = getBody(client.send(basicRequest.get(uri"$root")))
        decode[List[ToDo]](getResponseBody) match
            case Left(err) => fail(err.toString())
            case Right(l)  =>
                assertEquals(l(0).title, "Foo")
                assertEquals(l(0).completed, false)
                assertEquals(l(1).title, "Bar")
                assertEquals(l(1).completed, false)
                assertEquals(l(1).uid - l(0).uid, 1L)

    test("delete and get gives empty"):
        val delResponse = client.send(basicRequest.delete(uri"$root"))
        assert(delResponse.code.isSuccess)
        val getResponse = client.send(basicRequest.get(uri"$root"))
        assertResponseBodyIs(getResponse, "[]")
