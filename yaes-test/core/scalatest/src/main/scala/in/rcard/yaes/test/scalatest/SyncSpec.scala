package in.rcard.yaes.test.scalatest

import in.rcard.yaes.{Executor, Sync}

import scala.concurrent.Future

/** Mixin trait providing a stub [[Sync]] given instance for ScalaTest specs.
  *
  * Mix this trait into a ScalaTest spec class to get the [[withSync]] helper.
  * [[Sync.apply]] is identity, so the [[Executor]] is never invoked. The stub
  * executor exists only to satisfy [[Sync.Unsafe]] and fails loudly if called.
  * Programs run on the calling thread; exceptions propagate directly.
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
