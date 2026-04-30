package in.rcard.yaes.http.server

import in.rcard.yaes.http.core.BodyEncoder
import in.rcard.yaes.http.core.Headers
import java.util.Locale

/** HTTP response representation.
  *
  * Immutable case class representing an HTTP response to be sent back to the client. Use factory
  * methods in the companion object to construct instances.
  *
  * Example:
  * {{{
  * val response = Response.ok("Hello, World!")
  * val custom   = Response.withStatus(201, user, extraHeaders = Map("location" -> "/users/123"))
  * }}}
  *
  * @param status
  *   HTTP status code (200, 404, 500, etc.)
  * @param headers
  *   Response headers as a Map of header name to value
  * @param body
  *   Response body as a String
  */
case class Response private[server] (
    status: Int,
    headers: Map[String, String] = Map.empty,
    body: String = ""
)

object Response {

  /** Creates a 200 OK response with encoded body and automatic Content-Type.
    *
    * The encoder is resolved automatically from the context using Scala 3's `using` clauses. The
    * Content-Type header is set based on the encoder's specification. If `extraHeaders` contains
    * `Content-Type`, the caller's value overrides the encoder default.
    *
    * Example:
    * {{{
    * Response.ok("Hello, World!")  // Uses BodyEncoder[String] for text/plain
    * Response.ok(user)              // Uses BodyEncoder[User] with application/json
    * Response.ok(42)                // Uses BodyEncoder[Int] for text/plain
    * Response.ok(data, extraHeaders = Map("x-request-id" -> "abc123"))
    * }}}
    *
    * @param value
    *   The value to encode as the response body
    * @param extraHeaders
    *   Additional headers to include; caller's Content-Type overrides encoder default
    * @tparam A
    *   The type of the value
    * @return
    *   A Response with status 200 and appropriate Content-Type
    */
  inline def ok[A](value: A, extraHeaders: Map[String, String] = Map.empty)(using encoder: BodyEncoder[A]): Response =
    withStatus(200, value, extraHeaders)

  /** Creates a 201 Created response with encoded body.
    *
    * If `extraHeaders` contains `Content-Type`, the caller's value overrides the encoder default.
    *
    * Example:
    * {{{
    * Response.created(user, extraHeaders = Map("location" -> s"/users/${user.id}"))
    * }}}
    *
    * @param value
    *   The value to encode as the response body
    * @param extraHeaders
    *   Additional headers to include; caller's Content-Type overrides encoder default
    * @tparam A
    *   The type of the value
    * @return
    *   A Response with status 201 and appropriate Content-Type
    */
  inline def created[A](value: A, extraHeaders: Map[String, String] = Map.empty)(using encoder: BodyEncoder[A]): Response =
    withStatus(201, value, extraHeaders)

  /** Creates a 202 Accepted response with encoded body.
    *
    * If `extraHeaders` contains `Content-Type`, the caller's value overrides the encoder default.
    *
    * Example:
    * {{{
    * Response.accepted(task, extraHeaders = Map("x-task-id" -> task.id))
    * }}}
    *
    * @param value
    *   The value to encode as the response body
    * @param extraHeaders
    *   Additional headers to include; caller's Content-Type overrides encoder default
    * @tparam A
    *   The type of the value
    * @return
    *   A Response with status 202 and appropriate Content-Type
    */
  inline def accepted[A](value: A, extraHeaders: Map[String, String] = Map.empty)(using encoder: BodyEncoder[A]): Response =
    withStatus(202, value, extraHeaders)

  /** Creates a 204 No Content response.
    *
    * No body or encoder; can carry headers such as `ETag`.
    *
    * Example:
    * {{{
    * Response.noContent(extraHeaders = Map("etag" -> "\"abc123\""))
    * }}}
    *
    * @param extraHeaders
    *   Additional headers to include (e.g. ETag)
    * @return
    *   A Response with status 204 and empty body
    */
  inline def noContent(extraHeaders: Map[String, String] = Map.empty): Response =
    Response(status = 204, headers = extraHeaders.map((k, v) => k.toLowerCase(Locale.ROOT) -> v))

  /** Creates a 400 Bad Request response.
    *
    * If `extraHeaders` contains `Content-Type`, the caller's value overrides the encoder default.
    *
    * @param value
    *   The value to encode as the error message
    * @param extraHeaders
    *   Additional headers to include; caller's Content-Type overrides encoder default
    * @tparam A
    *   The type of the value
    * @return
    *   A Response with status 400 and appropriate Content-Type
    */
  inline def badRequest[A](value: A, extraHeaders: Map[String, String] = Map.empty)(using encoder: BodyEncoder[A]): Response =
    withStatus(400, value, extraHeaders)

  /** Creates a 404 Not Found response.
    *
    * If `extraHeaders` contains `Content-Type`, the caller's value overrides the encoder default.
    *
    * @param value
    *   The value to encode as the error message
    * @param extraHeaders
    *   Additional headers to include; caller's Content-Type overrides encoder default
    * @tparam A
    *   The type of the value
    * @return
    *   A Response with status 404 and appropriate Content-Type
    */
  inline def notFound[A](value: A, extraHeaders: Map[String, String] = Map.empty)(using encoder: BodyEncoder[A]): Response =
    withStatus(404, value, extraHeaders)

  /** Creates a 500 Internal Server Error response.
    *
    * If `extraHeaders` contains `Content-Type`, the caller's value overrides the encoder default.
    *
    * @param value
    *   The value to encode as the error message
    * @param extraHeaders
    *   Additional headers to include; caller's Content-Type overrides encoder default
    * @tparam A
    *   The type of the value
    * @return
    *   A Response with status 500 and appropriate Content-Type
    */
  inline def internalServerError[A](value: A, extraHeaders: Map[String, String] = Map.empty)(using encoder: BodyEncoder[A]): Response =
    withStatus(500, value, extraHeaders)

  /** Creates a 503 Service Unavailable response.
    *
    * Indicates the server is temporarily unable to handle the request, typically used during
    * graceful shutdown. If `extraHeaders` contains `Content-Type`, the caller's value overrides
    * the encoder default.
    *
    * Example:
    * {{{
    * Response.serviceUnavailable("Try again later", extraHeaders = Map("retry-after" -> "30"))
    * }}}
    *
    * @param value
    *   The value to encode as the error message
    * @param extraHeaders
    *   Additional headers to include; caller's Content-Type overrides encoder default
    * @tparam A
    *   The type of the value
    * @return
    *   A Response with status 503 and appropriate Content-Type
    */
  inline def serviceUnavailable[A](value: A, extraHeaders: Map[String, String] = Map.empty)(using encoder: BodyEncoder[A]): Response =
    withStatus(503, value, extraHeaders)

  /** Creates a response with any HTTP status code.
    *
    * Covers status codes not provided by the convenience methods (301, 206, 418, etc.).
    * If `extraHeaders` contains `Content-Type`, the caller's value overrides the encoder default.
    * Header names in `extraHeaders` are normalized to lowercase automatically.
    *
    * Example:
    * {{{
    * Response.withStatus(301, "", extraHeaders = Map("location" -> "/new-path"))
    * Response.withStatus(206, partialData, extraHeaders = Map("content-range" -> "bytes 0-499/1000"))
    * }}}
    *
    * @param status
    *   HTTP status code
    * @param value
    *   The value to encode as the response body
    * @param extraHeaders
    *   Additional headers to include; caller's Content-Type overrides encoder default
    * @tparam A
    *   The type of the value
    * @return
    *   A Response with the given status code and appropriate Content-Type
    */
  def withStatus[A](status: Int, value: A, extraHeaders: Map[String, String] = Map.empty)(using encoder: BodyEncoder[A]): Response =
    Response(
      status = status,
      headers = Map(Headers.ContentType -> encoder.contentType) ++ extraHeaders.map((k, v) => k.toLowerCase(Locale.ROOT) -> v),
      body = encoder.encode(value)
    )
}
