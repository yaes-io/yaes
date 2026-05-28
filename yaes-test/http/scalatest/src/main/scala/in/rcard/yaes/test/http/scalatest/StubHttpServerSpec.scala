package in.rcard.yaes.test.http.scalatest

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

/** ScalaTest mixin that provides a shared [[StubHttpServer]] for a test suite.
  *
  * Mix this trait into any ScalaTest `Suite` to get a running stub server that is automatically
  * stopped after the whole suite completes and reset (requests cleared, handler restored) before
  * each individual test.
  *
  * Example:
  * {{{
  * class MyHttpClientSpec
  *     extends AnyFlatSpec
  *     with Matchers
  *     with StubHttpServerSpec {
  *
  *   "MyHttpClient" should "call the correct endpoint" in {
  *     stubServer.setHandler(_ => StubResponse(200, """{"status":"ok"}"""))
  *     // … invoke the client under test pointing at stubBaseUrl …
  *     stubServer.capturedRequests.head.path shouldBe "/api/resource"
  *   }
  * }
  * }}}
  */
trait StubHttpServerSpec extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>

  /** The shared [[StubHttpServer]] instance for this suite. */
  val stubServer: StubHttpServer = new StubHttpServer()

  /** Convenience accessor for the base URL of the stub server (e.g. `http://localhost:54321`). */
  def stubBaseUrl: String = stubServer.baseUrl

  /** Stops the stub server after all tests in the suite have run.
    *
    * We reset before stopping so that any deferred handler failure from the final test is surfaced
    * deterministically during suite teardown.
    */
  abstract override def afterAll(): Unit = {
    var teardownError: Throwable = null

    try super.afterAll()
    catch {
      case t: Throwable =>
        teardownError = t
    }

    try stubServer.reset()
    catch {
      case t: Throwable =>
        if teardownError == null then teardownError = t
        else teardownError.addSuppressed(t)
    }

    try stubServer.stop()
    catch {
      case t: Throwable =>
        if teardownError == null then teardownError = t
        else teardownError.addSuppressed(t)
    }

    if teardownError != null then throw teardownError
  }

  /** Resets the stub server (clears captured requests and restores the default handler) before each
    * test.
    */
  abstract override def beforeEach(): Unit =
    stubServer.reset()
    super.beforeEach()
}
