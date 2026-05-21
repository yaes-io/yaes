package in.rcard.yaes.test.http.scalatest

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StubHttpServerTest
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with StubHttpServerSpec {

  private val httpClient: HttpClient = HttpClient.newHttpClient()

  "StubHttpServer" should "capture an incoming GET request" in {
    stubServer.setHandler(_ => StubResponse(200, "ok"))
    val request = HttpRequest
      .newBuilder(URI.create(s"$stubBaseUrl/test-path?foo=bar"))
      .GET()
      .build()
    httpClient.send(request, BodyHandlers.ofString())

    val captured = stubServer.capturedRequests
    captured should have size 1
    captured.head.method   shouldBe "GET"
    captured.head.path     shouldBe "/test-path"
    captured.head.rawQuery shouldBe Some("foo=bar")
  }

  it should "return the response configured via setHandler" in {
    stubServer.setHandler(_ => StubResponse(200, "hello world"))
    val request = HttpRequest
      .newBuilder(URI.create(s"$stubBaseUrl/ping"))
      .GET()
      .build()
    val response = httpClient.send(request, BodyHandlers.ofString())

    response.statusCode() shouldBe 200
    response.body()       shouldBe "hello world"
  }

  it should "clear captured requests and restore the default handler after reset" in {
    stubServer.setHandler(_ => StubResponse(200, "before reset"))
    val request = HttpRequest
      .newBuilder(URI.create(s"$stubBaseUrl/before"))
      .GET()
      .build()
    httpClient.send(request, BodyHandlers.ofString())
    stubServer.capturedRequests should have size 1

    stubServer.reset()

    stubServer.capturedRequests shouldBe empty

    val request2 = HttpRequest
      .newBuilder(URI.create(s"$stubBaseUrl/after"))
      .GET()
      .build()
    val response2 = httpClient.send(request2, BodyHandlers.ofString())
    response2.statusCode() shouldBe 500
    stubServer.capturedRequests should have size 1
  }
}
