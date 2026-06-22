# yaes-http-test-scalatest

ScalaTest utilities for testing HTTP interactions in the λÆS project. Provides a lightweight
in-process stub HTTP server backed by the JDK's built-in `com.sun.net.httpserver.HttpServer`.

## Installation

Add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "io.yaes" %% "yaes-http-test-scalatest" % "<version>" % Test
```

## Usage

Mix `StubHttpServerSpec` into your ScalaTest suite. The server binds to an ephemeral port on
construction, resets automatically before each test, and stops after the suite completes.

```scala
import io.yaes.test.http.scalatest.{StubHttpServer, StubHttpServerSpec, StubResponse}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.net.URI
import java.net.http.{HttpClient, HttpRequest}
import java.net.http.HttpResponse.BodyHandlers

class MyServiceSpec
    extends AnyFlatSpec
    with Matchers
    with StubHttpServerSpec {

  private val httpClient = HttpClient.newHttpClient()

  "MyService" should "call the correct endpoint" in {
    stubServer.setHandler(_ => StubResponse(200, """{"status":"ok"}""",
      Map("Content-Type" -> "application/json")))

    val request = HttpRequest
      .newBuilder(URI.create(s"$stubBaseUrl/api/resource"))
      .GET()
      .build()
    val response = httpClient.send(request, BodyHandlers.ofString())

    response.statusCode()                    shouldBe 200
    stubServer.capturedRequests.head.path    shouldBe "/api/resource"
    stubServer.capturedRequests.head.method  shouldBe "GET"
  }
}
```

### Key types

| Type | Description |
|------|-------------|
| `StubHttpServer` | The stub server. Exposes `port`, `baseUrl`, `setHandler`, `capturedRequests`, `reset`, and `stop`. |
| `CapturedRequest` | Immutable snapshot of a received request (method, path, rawQuery, headers, body). |
| `StubResponse` | Describes the response to send back (statusCode, body, headers). |
| `StubHttpServerSpec` | ScalaTest mixin that manages the server lifecycle automatically. |
