import sttp.client3.{SimpleHttpClient, UriContext, basicRequest}

class MySuite extends munit.FunSuite {
	val path = "http://localhost:8080"
	test("root works") {
		val client = SimpleHttpClient()
		val response = client.send(basicRequest.get(uri"$path"))
		assert(response.code.isSuccess)
		assert(response.body.contains("Site is running :)"))
	}
}
