import munit.Assertions.fail
import io.circe.generic.auto.*
import io.circe.parser.*
import sttp.client3.{basicRequest, Response, SimpleHttpClient, UriContext}
import org.http4s.circe.*

def toNativeUri(uri: org.http4s.Uri): sttp.model.Uri = uri"${uri.toString()}"

extension [T](response: Response[Either[T, String]])
    def getBodySafe(): String =
        assert(response.isSuccess)
        response.body.toOption.get

    def getAPIToDoSafe(): APIToDo =
        decode[APIToDo](response.getBodySafe()) match
            case Left(err)   => fail(err.toString())
            case Right(todo) => todo

    def getAPIToDoSeqSafe(): Seq[APIToDo] =
        decode[Seq[APIToDo]](response.getBodySafe()) match
            case Left(err)   => fail(err.toString())
            case Right(list) => list

class MySuite extends munit.FunSuite:
    val client = SimpleHttpClient()

    test("server is up"):
        assert(client.send(basicRequest.get(uri"${Constants.root}")).isSuccess)

    test("redis is up"):
        assertEquals(
            client
                .send(basicRequest.get(uri"${Constants.root}/ping"))
                .getBodySafe(),
            "PONG")

    def deleteAndGetTest() =
        assert(
            client.send(basicRequest.delete(uri"${Constants.root}")).isSuccess)

        assertEquals(
            client.send(basicRequest.get(uri"${Constants.root}")).getBodySafe(),
            "[]")

    test("data cleared successfully"):
        deleteAndGetTest()

    test("adding a new todo works"):
        val todo = client
            .send(
                basicRequest
                    .post(uri"${Constants.root}")
                    .body("{\"title\": \"Foo\"}"))
            .getAPIToDoSafe()
        assertEquals(todo.title, "Foo")
        assertEquals(todo.completed, false)

        assert(
            client
                .send(
                    basicRequest
                        .post(uri"${Constants.root}")
                        .body("{\"title\": \"Bar\"}"))
                .isSuccess)

        val todos = client
            .send(basicRequest.get(uri"${Constants.root}"))
            .getAPIToDoSeqSafe()
        assertEquals(
            Set(
                (todos(0).title, todos(0).completed),
                (todos(1).title, todos(1).completed),
            ),
            Set(("Foo", false), ("Bar", false)))

    test("can navigate to a single todo"):
        val todos = client
            .send(basicRequest.get(uri"${Constants.root}"))
            .getAPIToDoSeqSafe()
        todos.foreach(toDoAtRoot =>
            val toDoAtOwn = client
                .send(basicRequest.get(toNativeUri(toDoAtRoot.url)))
                .getAPIToDoSafe()
            assertEquals(toDoAtRoot, toDoAtOwn),
        )

    def patchTest(
        postBody: String,
        postAss: APIToDo => Unit,
        patchBody: String,
        patchAss: APIToDo => Unit,
    ): Unit =
        val todo = client
            .send(basicRequest.post(uri"${Constants.root}").body(postBody))
            .getAPIToDoSafe()
        postAss(todo)
        val url  = toNativeUri(todo.url)

        val todo2 = client
            .send(basicRequest.patch(url).body(patchBody))
            .getAPIToDoSafe()
        patchAss(todo2)

        val todo3 = client.send(basicRequest.get(url)).getAPIToDoSafe()
        assertEquals(todo2, todo3)

    test("can update title and completedness"):
        patchTest(
            "{\"title\": \"Important\"}",
            todo => assert(!todo.completed),
            "{\"completed\": true, \"title\": \"Not important\"}",
            todo2 =>
                assertEquals(todo2.title, "Not important")
                assert(todo2.completed),
        )

        val todos = client
            .send(basicRequest.get(uri"${Constants.root}"))
            .getAPIToDoSeqSafe()
        assert(
            todos
                .map(t => (t.title, t.completed))
                .contains(("Not important", true)))

    test("can update order from null"):
        patchTest(
            "{\"title\": \"Foo\"}",
            todo => assertEquals(todo.order, None),
            "{\"order\": 10}",
            todo2 => assertEquals(todo2.order, Some(10)))

    test("can update order from value"):
        patchTest(
            "{\"title\": \"Bar\", \"order\": 21}",
            todo => assertEquals(todo.order, Some(21)),
            "{\"order\": 0}",
            todo2 => assertEquals(todo2.order, Some(0)))

    test("can delete a todo"):
        val url = toNativeUri(
            client
                .send(
                    basicRequest
                        .post(uri"${Constants.root}")
                        .body("{\"title\": \"Useless\"}"))
                .getAPIToDoSafe()
                .url)

        assert(client.send(basicRequest.delete(url)).isSuccess)

        assertEquals(client.send(basicRequest.get(url)).code.code, 404)

        assert(client.send(basicRequest.get(uri"${Constants.root}")).isSuccess)

    test("delete all works"):
        deleteAndGetTest()

    test("double delete all works"):
        deleteAndGetTest()
