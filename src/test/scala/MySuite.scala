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

    def deleteAndGetTest() =
        val delResponse = client.send(basicRequest.delete(uri"$root"))
        assert(delResponse.code.isSuccess)
        val getResponse = client.send(basicRequest.get(uri"$root"))
        assertResponseBodyIs(getResponse, "[]")

    test("server is up"):
        val response = client.send(basicRequest.get(uri"$root"))
        assert(response.isSuccess)

    test("redis is up"):
        val response = client.send(basicRequest.get(uri"$root/ping"))
        assertResponseBodyIs(response, "PONG")

    test("data cleared successfully"):
        deleteAndGetTest()

    test("adding a new todo works"):
        val postResponse1Body = getBody(
            client.send(
                basicRequest.post(uri"$root").body("{\"title\": \"Foo\"}")))

        decode[ToDo](postResponse1Body) match
            case Left(err)   => fail(err.toString())
            case Right(todo) =>
                assertEquals(todo.title, "Foo")
                assertEquals(todo.completed, false)

        client.send(basicRequest.post(uri"$root").body("{\"title\": \"Bar\"}"))

        val getResponseBody = getBody(client.send(basicRequest.get(uri"$root")))
        decode[List[ToDo]](getResponseBody) match
            case Left(err) => fail(err.toString())
            case Right(l)  =>
                assertEquals(l(0).title, "Foo")
                assertEquals(l(0).completed, false)
                assertEquals(l(1).title, "Bar")
                assertEquals(l(1).completed, false)
                assertEquals(l(1).uid - l(0).uid, 1L)

    test("can navigate to a single todo"):
        val getRootResponseBody =
            getBody(client.send(basicRequest.get(uri"$root")))
        decode[List[APIToDo]](getRootResponseBody) match
            case Left(outerErr) => fail(outerErr.toString())
            case Right(l)       =>
                l.foreach(toDoAtRoot =>
                    val url                 = root + toDoAtRoot.url
                    val getToDoResponseBody =
                        getBody(client.send(basicRequest.get(uri"$url")))
                    decode[APIToDo](getToDoResponseBody) match
                        case Left(innerErr)   => fail(innerErr.toString())
                        case Right(toDoOnOwn) =>
                            assertEquals(toDoAtRoot, toDoOnOwn),
                )

    test("delete all works"):
        deleteAndGetTest()

    test("double delete works"):
        deleteAndGetTest()
