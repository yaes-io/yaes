package in.rcard.yaes.test.scalatest

import in.rcard.yaes.Raise
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.exceptions.TestFailedException

class RaiseSpecTest extends AnyFlatSpec with Matchers with RaiseSpec {

  "failOnRaise" should "return the value when no error is raised" in {
    val result = failOnRaise[String, Int] {
      42
    }
    result shouldBe 42
  }

  it should "throw TestFailedException with correct message when an error is raised" in {
    val ex = intercept[TestFailedException] {
      failOnRaise[String, Int] {
        Raise.raise("oops")
      }
    }
    ex.getMessage shouldBe "Expected the test not to raise any errors but it did with error 'oops'"
  }

  "interceptRaised" should "return the error when it is raised" in {
    val error = interceptRaised[String, Int] {
      Raise.raise("expected error")
    }
    error shouldBe "expected error"
  }

  it should "throw TestFailedException with correct message when no error is raised" in {
    val ex = intercept[TestFailedException] {
      interceptRaised[String, Int] {
        42
      }
    }
    ex.getMessage shouldBe "Expected an error to be raised but body evaluated successfully"
  }

  it should "work with union error types" in {
    val error = interceptRaised[String | Int, Boolean] {
      Raise.raise("union error")
    }
    error shouldBe "union error"
  }
}
