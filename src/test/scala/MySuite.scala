import sttp.client3.{SimpleHttpClient, UriContext, basicRequest}

class MySuite extends munit.FunSuite {
	val root = "http://localhost:8080"
	test("server is up") {
		val client = SimpleHttpClient()
		val response = client.send(basicRequest.get(uri"$root"))
		assert(response.code.isSuccess)
	}

	test("redis is up") {
		val client = SimpleHttpClient()
		val response = client.send(basicRequest.get(uri"$root/ping"))
		assert(response.code.isSuccess)
		assert(response.body.contains("PONG"))
	}
}
