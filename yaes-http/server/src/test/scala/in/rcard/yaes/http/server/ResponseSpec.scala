package in.rcard.yaes.http.server


import in.rcard.yaes.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import in.rcard.yaes.http.core.Headers

class ResponseSpec extends AnyFlatSpec with Matchers {

  "Response.ok" should "create a 200 response with text/plain content type for String" in {
    val response = Response.ok("Hello!")

    response.status shouldBe 200
    response.body shouldBe "Hello!"
    response.headers should contain(Headers.ContentType -> "text/plain; charset=UTF-8")
  }

  it should "set Content-Type from encoder for Int" in {
    val response = Response.ok(42)

    response.status shouldBe 200
    response.body shouldBe "42"
    response.headers should contain(Headers.ContentType -> "text/plain; charset=UTF-8")
  }

  it should "add extra headers to the response" in {
    val response = Response.ok("Hello!", extraHeaders = Map("X-Request-Id" -> "abc123"))

    response.status shouldBe 200
    response.headers should contain(Headers.ContentType -> "text/plain; charset=UTF-8")
    response.headers should contain("x-request-id" -> "abc123")
    response.headers.count { case (name, _) => name.equalsIgnoreCase("x-request-id") } shouldBe 1
  }

  it should "override Content-Type via extraHeaders" in {
    val response = Response.ok("raw", extraHeaders = Map(Headers.ContentType -> "application/json"))

    response.status shouldBe 200
    response.headers(Headers.ContentType) shouldBe "application/json"
  }

  it should "override Content-Type via extraHeaders when using mixed-case header name" in {
    val response = Response.ok("raw", extraHeaders = Map("Content-Type" -> "application/json"))

    response.status shouldBe 200
    response.headers(Headers.ContentType) shouldBe "application/json"
    response.headers.count { case (name, _) => name.equalsIgnoreCase(Headers.ContentType) } shouldBe 1
  }

  "Response.created" should "create a 201 response" in {
    val response = Response.created("Resource created")

    response.status shouldBe 201
    response.body shouldBe "Resource created"
    response.headers should contain(Headers.ContentType -> "text/plain; charset=UTF-8")
  }

  it should "encode value and set headers" in {
    val response = Response.created("test")

    response.status shouldBe 201
    response.body shouldBe "test"
    response.headers should contain(Headers.ContentType -> "text/plain; charset=UTF-8")
  }

  it should "add extra headers" in {
    val response = Response.created("created", extraHeaders = Map("location" -> "/users/42"))

    response.status shouldBe 201
    response.headers should contain("location" -> "/users/42")
    response.headers should contain(Headers.ContentType -> "text/plain; charset=UTF-8")
  }

  "Response.accepted" should "create a 202 response" in {
    val response = Response.accepted("Processing")

    response.status shouldBe 202
    response.body shouldBe "Processing"
  }

  it should "add extra headers" in {
    val response = Response.accepted("ok", extraHeaders = Map("x-task-id" -> "t1"))

    response.status shouldBe 202
    response.headers should contain("x-task-id" -> "t1")
  }

  "Response.noContent" should "create a 204 response with empty body" in {
    val response = Response.noContent()

    response.status shouldBe 204
    response.body shouldBe ""
    response.headers shouldBe Map.empty
  }

  it should "add extra headers (e.g. ETag)" in {
    val response = Response.noContent(extraHeaders = Map("etag" -> "\"abc123\""))

    response.status shouldBe 204
    response.body shouldBe ""
    response.headers should contain("etag" -> "\"abc123\"")
  }

  "Response.badRequest" should "create a 400 response" in {
    val response = Response.badRequest("Invalid input")

    response.status shouldBe 400
    response.body shouldBe "Invalid input"
  }

  it should "add extra headers" in {
    val response = Response.badRequest("error", extraHeaders = Map("x-error-code" -> "E001"))

    response.status shouldBe 400
    response.headers should contain("x-error-code" -> "E001")
  }

  "Response.notFound" should "create a 404 response" in {
    val response = Response.notFound("Resource not found")

    response.status shouldBe 404
    response.body shouldBe "Resource not found"
  }

  it should "add extra headers" in {
    val response = Response.notFound("not found", extraHeaders = Map("x-hint" -> "check-id"))

    response.status shouldBe 404
    response.headers should contain("x-hint" -> "check-id")
  }

  "Response.internalServerError" should "create a 500 response" in {
    val response = Response.internalServerError("Database error")

    response.status shouldBe 500
    response.body shouldBe "Database error"
  }

  it should "add extra headers" in {
    val response = Response.internalServerError("error", extraHeaders = Map("x-trace-id" -> "t123"))

    response.status shouldBe 500
    response.headers should contain("x-trace-id" -> "t123")
  }

  "Response.serviceUnavailable" should "create a 503 response" in {
    val response = Response.serviceUnavailable("Try again later")

    response.status shouldBe 503
    response.body shouldBe "Try again later"
    response.headers should contain(Headers.ContentType -> "text/plain; charset=UTF-8")
  }

  it should "add extra headers" in {
    val response = Response.serviceUnavailable("unavailable", extraHeaders = Map("retry-after" -> "30"))

    response.status shouldBe 503
    response.headers should contain("retry-after" -> "30")
  }

  "Response.withStatus" should "create response with custom status code" in {
    val response = Response.withStatus(301, "")

    response.status shouldBe 301
    response.body shouldBe ""
  }

  it should "add extra headers" in {
    val response = Response.withStatus(301, "", extraHeaders = Map("location" -> "/new-path"))

    response.status shouldBe 301
    response.headers should contain("location" -> "/new-path")
  }

  it should "override Content-Type via extraHeaders" in {
    val response = Response.withStatus(206, "partial", extraHeaders = Map(Headers.ContentType -> "application/octet-stream"))

    response.status shouldBe 206
    response.headers(Headers.ContentType) shouldBe "application/octet-stream"
  }

  "Response case class" should "allow custom headers" in {
    val response = Response(
      status = 200,
      headers = Map("x-custom" -> "value", Headers.ContentType -> "application/json"),
      body = """{"key": "value"}"""
    )

    response.status shouldBe 200
    response.headers should contain("x-custom" -> "value")
    response.headers should contain(Headers.ContentType -> "application/json")
  }

  it should "default headers to empty map" in {
    val response = Response(200, body = "OK")

    response.headers shouldBe Map.empty
  }

  it should "default body to empty string" in {
    val response = Response(204)

    response.body shouldBe ""
  }
}
