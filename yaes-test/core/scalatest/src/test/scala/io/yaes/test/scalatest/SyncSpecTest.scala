package io.yaes.test.scalatest

import io.yaes.Sync
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SyncSpecTest extends AnyFlatSpec with Matchers with SyncSpec {

  "withSync" should "return the result of a simple computation" in {
    val result = withSync {
      42
    }
    result shouldBe 42
  }

  it should "provide a Sync context parameter to the program" in {
    val result = withSync {
      Sync[String] { "hello" }
    }
    result shouldBe "hello"
  }

  it should "propagate exceptions thrown by the program" in {
    val ex = intercept[RuntimeException] {
      withSync {
        throw new RuntimeException("boom")
      }
    }
    ex.getMessage shouldBe "boom"
  }

  it should "propagate custom exception types" in {
    case class DomainException(msg: String) extends Exception(msg)
    intercept[DomainException] {
      withSync {
        throw DomainException("domain error")
      }
    }
  }

  it should "execute the program synchronously and return the computed value" in {
    var sideEffect = 0
    val result = withSync {
      sideEffect = 1
      sideEffect * 10
    }
    sideEffect shouldBe 1
    result shouldBe 10
  }
}
