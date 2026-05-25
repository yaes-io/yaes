package in.rcard.yaes.test.scalatest

import in.rcard.yaes.{Executor, Sync}

import scala.concurrent.Future
import scala.util.Try

/** Mixin trait providing a synchronous [[Sync]] runner for ScalaTest specs.
  *
  * Mix this trait into a ScalaTest spec class to get the [[withSync]] helper. Unlike the
  * production [[Sync.run]] handler, [[withSync]] executes the program synchronously on the calling
  * thread inside a `Try`. No virtual threads, no Future awaiting. This keeps test execution simple
  * and deterministic while still satisfying `using Sync` context parameters.
  *
  * Example:
  * {{{
  * class MySpec extends AnyFlatSpec with Matchers with SyncSpec {
  *
  *   "withSync" should "return the computed value" in {
  *     val result = withSync {
  *       42
  *     }
  *     result shouldBe 42
  *   }
  *
  *   it should "propagate exceptions thrown by the program" in {
  *     intercept[RuntimeException] {
  *       withSync {
  *         throw new RuntimeException("boom")
  *       }
  *     }
  *   }
  * }
  * }}}
  */
trait SyncSpec {

  /** Runs a [[Sync]]-effectful program synchronously and returns its result.
    *
    * The program is evaluated on the calling thread inside a `Try`. No virtual threads or thread
    * pools are used. Any exception thrown by the program propagates directly to the caller.
    *
    * @param program
    *   The effectful computation to run.
    * @tparam A
    *   The result type of the computation.
    * @return
    *   The result of the computation.
    * @throws Throwable
    *   if the program throws an exception.
    */
  def withSync[A](program: Sync ?=> A): A = runSync(program)

  private def runSync[A](program: Sync ?=> A): A = {
    val syncInstance = new Sync.Unsafe {
      override val executor: Executor = new Executor {
        override def submit[A](task: => A): Future[A] =
          Try(task).fold(Future.failed(_), Future.successful)
      }
    }
    given Sync = syncInstance
    Try(program).fold(throw _, identity)
  }
}
