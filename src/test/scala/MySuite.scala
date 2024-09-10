import munit.Assertions._

import sttp.client3.{SimpleHttpClient, UriContext, basicRequest}
import sttp.client3.Response

def assertResponseBodyIs[W, T](response: Response[Either[W, T]], target: T) =
	response.body match
		case Left(_)    => fail(s"Received HTTP error ${response.code}")
		case Right(body) => assertEquals(body, target)

class MySuite extends munit.FunSuite:
	val root = "http://localhost:8080"
	val client = SimpleHttpClient()

	test("server is up"):
		val response = client.send(basicRequest.get(uri"$root"))
		assert(response.isSuccess)

	test("redis is up"):
		val response = client.send(basicRequest.get(uri"$root/ping"))
		assertResponseBodyIs(response, "PONG")

	test("post gives added"):
		val postResponse = client.send(basicRequest.post(uri"$root"))
		assertResponseBodyIs(postResponse, ???)

	test("delete and get gives empty"):
		val delResponse = client.send(basicRequest.delete(uri"$root"))
		assert(delResponse.code.isSuccess)
		val getResponse = client.send(basicRequest.get(uri"$root"))
		assertResponseBodyIs(getResponse, "[]")