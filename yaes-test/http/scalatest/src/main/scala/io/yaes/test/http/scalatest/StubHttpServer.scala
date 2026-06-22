package io.yaes.test.http.scalatest

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import scala.jdk.CollectionConverters.*

/** Represents an HTTP request captured by the [[StubHttpServer]].
  *
  * @param method
  *   the HTTP method (e.g. `GET`, `POST`)
  * @param path
  *   the raw request path (e.g. `/api/users`)
  * @param rawQuery
  *   the raw query string, if present (e.g. `foo=bar&baz=qux`)
  * @param headers
  *   the request headers, with lower-cased header names mapped to their list of values
  * @param body
  *   the request body decoded as a UTF-8 string
  */
case class CapturedRequest(
    method: String,
    path: String,
    rawQuery: Option[String],
    headers: Map[String, List[String]],
    body: String
)

/** Describes the response that [[StubHttpServer]] should send back to a client.
  *
  * @param statusCode
  *   the HTTP status code to respond with (e.g. `200`, `404`)
  * @param body
  *   the response body as a plain string; encoded to UTF-8 bytes before sending
  * @param headers
  *   additional response headers to include (default: empty)
  */
case class StubResponse(
    statusCode: Int,
    body: String,
    headers: Map[String, String] = Map.empty
)

/** A lightweight in-process HTTP stub server backed by the JDK's built-in
  * `com.sun.net.httpserver.HttpServer`.
  *
  * On construction the server binds to an ephemeral port and immediately starts accepting
  * connections. It captures every incoming request and delegates response generation to a
  * configurable handler that can be swapped at runtime via [[setHandler]].
  *
  * The server is designed for use in tests via the [[StubHttpServerSpec]] mixin. A typical
  * lifecycle is:
  *   - construct once for the test suite
  *   - call [[reset]] before each test to clear captured requests and restore the default handler
  *   - call [[stop]] after the entire suite finishes
  *
  * Example:
  * {{{
  * val server = new StubHttpServer()
  * server.setHandler(_ => StubResponse(200, """{"ok":true}""", Map("Content-Type" -> "application/json")))
  * // … make HTTP requests to server.baseUrl …
  * val requests = server.capturedRequests
  * server.stop()
  * }}}
  */
class StubHttpServer {
  private val defaultHandler: CapturedRequest => StubResponse =
    _ => StubResponse(500, "no handler configured")

  private val handlerRef    = new AtomicReference[CapturedRequest => StubResponse](defaultHandler)
  private val requestsQueue = new ConcurrentLinkedQueue[CapturedRequest]()
  private val handlerError  = new AtomicReference[Option[Throwable]](None)

  private val jdkServer: HttpServer =
    val s = HttpServer.create(new InetSocketAddress(0), 0)
    s.createContext(
      "/",
      (exchange: HttpExchange) => {
        try
          val bodyBytes = exchange.getRequestBody.readAllBytes()
          val body      = new String(bodyBytes, UTF_8)
          val uri       = exchange.getRequestURI
          val captured = CapturedRequest(
            method = exchange.getRequestMethod,
            path = uri.getRawPath,
            rawQuery = Option(uri.getRawQuery),
            headers = exchange.getRequestHeaders.entrySet().asScala.map { entry =>
              entry.getKey.toLowerCase(Locale.ROOT) -> entry.getValue.asScala.toList
            }.toMap,
            body = body
          )
          requestsQueue.add(captured)
          val response =
            try handlerRef.get()(captured)
            catch
              case ex: Throwable =>
                handlerError.set(Some(ex))
                StubResponse(500, "handler error")
          val responseBytes = response.body.getBytes(UTF_8)
          response.headers.foreach { (k, v) => exchange.getResponseHeaders.set(k, v) }
          exchange.sendResponseHeaders(response.statusCode, responseBytes.length)
          val os = exchange.getResponseBody
          try os.write(responseBytes)
          finally os.close()
        finally exchange.close()
      }
    )
    s.setExecutor(null)
    s.start()
    s

  /** The ephemeral port on which this server is listening. */
  val port: Int = jdkServer.getAddress.getPort

  /** The base URL of this server, e.g. `http://localhost:54321`. */
  val baseUrl: String = s"http://localhost:$port"

  /** Replaces the current request handler with the given function.
    *
    * The handler receives the [[CapturedRequest]] and must return a [[StubResponse]] that will be
    * sent back to the client. The replacement is thread-safe.
    *
    * @param handler
    *   a function from [[CapturedRequest]] to [[StubResponse]]
    */
  def setHandler(handler: CapturedRequest => StubResponse): Unit =
    handlerRef.set(handler)

  /** Returns all requests captured since the last [[reset]] (or since construction).
    *
    * If the request handler threw an exception during a previous invocation, that exception is
    * re-thrown here so test assertions on the server thread surface immediately in the test body.
    *
    * @return
    *   an ordered list of [[CapturedRequest]] values
    * @throws Throwable
    *   the exception thrown by the handler, if any
    */
  def capturedRequests: List[CapturedRequest] =
    handlerError.getAndSet(None).foreach(ex => throw ex)
    requestsQueue.asScala.toList

  /** Clears the captured-request queue and restores the default handler (which returns 500).
    *
    * If the request handler threw an exception during a previous invocation, that exception is
    * re-thrown here so failures on the server thread are not silently discarded between tests.
    *
    * Intended to be called before each test so that each test starts with a clean slate.
    *
    * @throws Throwable
    *   the exception thrown by the handler, if any
    */
  def reset(): Unit =
    val storedError = handlerError.getAndSet(None)
    requestsQueue.clear()
    handlerRef.set(defaultHandler)
    storedError.foreach(ex => throw ex)

  /** Stops the underlying JDK HTTP server, releasing the bound port.
    *
    * After calling [[stop]], the server no longer accepts connections. This should be called once
    * after all tests in a suite have finished.
    */
  def stop(): Unit = jdkServer.stop(0)
}
