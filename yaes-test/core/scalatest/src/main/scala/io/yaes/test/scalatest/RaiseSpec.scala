package io.yaes.test.scalatest

import io.yaes.Raise
import org.scalatest.exceptions.TestFailedException

/** Mixin trait providing test utilities for code that uses the [[Raise]] effect.
  *
  * Mix this trait into a ScalaTest spec class to get [[failOnRaise]] and [[interceptRaised]]
  * helpers. Both methods use `Raise.either` internally, so no `ClassTag` is required and union
  * error types are fully supported.
  *
  * Example:
  * {{{
  * class MySpec extends AnyFlatSpec with Matchers with RaiseSpec {
  *
  *   "divide" should "return the quotient" in {
  *     val result = failOnRaise[ArithmeticError, Int] {
  *       divide(10, 2)
  *     }
  *     result shouldBe 5
  *   }
  *
  *   it should "raise DivisionByZero for zero divisor" in {
  *     val error = interceptRaised[ArithmeticError, Int] {
  *       divide(10, 0)
  *     }
  *     error shouldBe DivisionByZero
  *   }
  * }
  * }}}
  */
trait RaiseSpec {

  /** Runs a [[Raise]]-effectful body and returns the successful result, failing the test if an
    * error is raised.
    *
    * Example:
    * {{{
    * val result = failOnRaise[String, Int] {
    *   computeValue()
    * }
    * result shouldBe 42
    * }}}
    *
    * @param body
    *   the effectful computation to run
    * @return
    *   the successful result of the computation
    * @throws TestFailedException
    *   if the body raises an error
    * @tparam E
    *   the error type
    * @tparam A
    *   the result type
    */
  def failOnRaise[E, A](body: Raise[E] ?=> A): A =
    Raise.either[E, A](body) match {
      case Right(a) => a
      case Left(e)  => throw new TestFailedException(
          s"Expected the test not to raise any errors but it did with error '$e'",
          1
        )
    }

  /** Runs a [[Raise]]-effectful body and returns the raised error, failing the test if the body
    * completes successfully.
    *
    * Example:
    * {{{
    * val error = interceptRaised[String, Int] {
    *   failingComputation()
    * }
    * error shouldBe "expected error"
    * }}}
    *
    * @param body
    *   the effectful computation expected to raise an error
    * @return
    *   the raised error value
    * @throws TestFailedException
    *   if the body does not raise an error
    * @tparam E
    *   the error type
    * @tparam A
    *   the result type
    */
  def interceptRaised[E, A](body: Raise[E] ?=> A): E =
    Raise.either[E, A](body) match {
      case Left(e)  => e
      case Right(_) => throw new TestFailedException(
          "Expected an error to be raised but body evaluated successfully",
          1
        )
    }
}
