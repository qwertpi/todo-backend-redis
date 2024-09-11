import munit.Assertions.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import sttp.client3.{basicRequest, SimpleHttpClient, UriContext}
import sttp.client3.Response
import org.http4s.Uri
import org.http4s.circe.*

def getBody[W, T](response: Response[Either[W, T]]): T =
    response.body match
        case Left(err)   => fail(s"HTTP error: ${err.toString()}")
        case Right(body) => body

def assertResponseBodyIs[W, T](response: Response[Either[W, T]], target: T) =
    response.body match
        case Left(err)   => fail(s"HTTP error: ${err.toString()}")
        case Right(body) => assertEquals(body, target)

class MySuite extends munit.FunSuite:
    val client = SimpleHttpClient()

    def deleteAndGetTest() =
        val delResponse =
            client.send(basicRequest.delete(uri"${Constants.root}"))
        assert(delResponse.code.isSuccess)
        val getResponse = client.send(basicRequest.get(uri"${Constants.root}"))
        assertResponseBodyIs(getResponse, "[]")

    test("server is up"):
        val response = client.send(basicRequest.get(uri"${Constants.root}"))
        assert(response.isSuccess)

    test("redis is up"):
        val response =
            client.send(basicRequest.get(uri"${Constants.root}/ping"))
        assertResponseBodyIs(response, "PONG")

    test("data cleared successfully"):
        deleteAndGetTest()

    test("adding a new todo works"):
        val postResponse1Body = getBody(
            client.send(
                basicRequest
                    .post(uri"${Constants.root}")
                    .body("{\"title\": \"Foo\"}")))

        decode[APIToDo](postResponse1Body) match
            case Left(err)   => fail(err.toString())
            case Right(todo) =>
                assertEquals(todo.title, "Foo")
                assertEquals(todo.completed, false)
        client.send(
            basicRequest
                .post(uri"${Constants.root}")
                .body("{\"title\": \"Bar\"}"))

        val getResponseBody =
            getBody(client.send(basicRequest.get(uri"${Constants.root}")))
        decode[List[ToDo]](getResponseBody) match
            case Left(err) => fail(err.toString())
            case Right(l)  =>
                assertEquals(
                    Set(
                        (l(0).title, l(0).completed),
                        (l(1).title, l(1).completed),
                    ),
                    Set(("Foo", false), ("Bar", false)))

    test("can navigate to a single todo"):
        val getRootResponseBody =
            getBody(client.send(basicRequest.get(uri"${Constants.root}")))
        decode[List[APIToDo]](getRootResponseBody) match
            case Left(outerErr) => fail(outerErr.toString())
            case Right(l)       =>
                l.foreach(toDoAtRoot =>
                    val getToDoResponseBody =
                        getBody(
                            client.send(basicRequest.get(
                                uri"${toDoAtRoot.url.toString()}")))
                    decode[APIToDo](getToDoResponseBody) match
                        case Left(innerErr)   => fail(innerErr.toString())
                        case Right(toDoOnOwn) =>
                            assertEquals(toDoAtRoot, toDoOnOwn),
                )

    test("delete all works"):
        deleteAndGetTest()

    test("double delete works"):
        deleteAndGetTest()
