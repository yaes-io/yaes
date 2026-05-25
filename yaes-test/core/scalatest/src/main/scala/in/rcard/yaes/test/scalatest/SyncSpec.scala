package in.rcard.yaes.test.scalatest

import in.rcard.yaes.{Executor, Sync}

import scala.concurrent.Future

/** Mixin trait providing a stub [[Sync]] given instance for ScalaTest specs.
  *
  * Mix this trait into a ScalaTest spec class to get the [[withSync]] helper.
  * [[withSync]] evaluates `Sync ?=> A` computations synchronously on the calling thread by
  * supplying a contextual `Sync` whose [[Sync.apply]] is the identity. Exceptions propagate
  * directly to the caller.
  *
  * '''Limitation:''' [[withSync]] is intended for computations that use `Sync.apply` directly
  * (e.g., `Sync { expr }`). It does '''not''' change the behavior of `Sync.run` or
  * `Sync.runBlocking`, which always use `Sync`'s internal `JvmExecutor` (virtual threads)
  * regardless of the contextual `given Sync`. If the program under test calls `Sync.run*`
  * internally, virtual threads may still be created.
  *
  * Example:
  * {{{
  * class MySpec extends AnyFlatSpec with Matchers with SyncSpec {
  *
  *   "withSync" should "return the computed value" in {
  *     val result = withSync { 42 }
  *     result shouldBe 42
  *   }
  *
  *   it should "propagate exceptions thrown by the program" in {
  *     intercept[RuntimeException] {
  *       withSync { throw new RuntimeException("boom") }
  *     }
  *   }
  * }
  * }}}
  */
trait SyncSpec {

  given Sync = new Sync.Unsafe {
    override val executor: Executor = new Executor {
      override def submit[A](task: => A): Future[A] =
        throw new UnsupportedOperationException("SyncSpec: Executor.submit must not be called in tests")
    }
  }

  def withSync[A](program: Sync ?=> A): A = program
}
