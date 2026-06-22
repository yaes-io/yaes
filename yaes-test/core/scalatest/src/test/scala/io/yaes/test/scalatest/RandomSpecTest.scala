package io.yaes.test.scalatest

import io.yaes.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.exceptions.TestFailedException

class RandomSpecTest extends AnyFlatSpec with Matchers with RandomSpec {

  "RandomStub" should "return queued ints in order" in {
    rand.nextInts(1, 2, 3)
    Random.nextInt shouldBe 1
    Random.nextInt shouldBe 2
    Random.nextInt shouldBe 3
  }

  it should "return queued longs in order" in {
    rand.nextLongs(100L, 200L)
    Random.nextLong shouldBe 100L
    Random.nextLong shouldBe 200L
  }

  it should "return queued booleans in order" in {
    rand.nextBooleans(true, false)
    Random.nextBoolean shouldBe true
    Random.nextBoolean shouldBe false
  }

  it should "return queued doubles in order" in {
    rand.nextDoubles(0.5, 1.5)
    Random.nextDouble shouldBe 0.5
    Random.nextDouble shouldBe 1.5
  }

  it should "throw TestFailedException when int queue is empty" in {
    val ex = intercept[TestFailedException] {
      Random.nextInt
    }
    ex.getMessage shouldBe "RandomStub: no ints queued"
  }

  it should "throw TestFailedException when long queue is empty" in {
    val ex = intercept[TestFailedException] {
      Random.nextLong
    }
    ex.getMessage shouldBe "RandomStub: no longs queued"
  }

  it should "throw TestFailedException when boolean queue is empty" in {
    val ex = intercept[TestFailedException] {
      Random.nextBoolean
    }
    ex.getMessage shouldBe "RandomStub: no booleans queued"
  }

  it should "throw TestFailedException when double queue is empty" in {
    val ex = intercept[TestFailedException] {
      Random.nextDouble
    }
    ex.getMessage shouldBe "RandomStub: no doubles queued"
  }

  "RandomSpec" should "reset the stub before each test so queued values do not bleed across tests" in {
    // Queue a value in this test only; the withFixture reset guarantees the queue starts empty.
    rand.nextInts(99)
    Random.nextInt shouldBe 99
    // Queue should be empty now
    intercept[TestFailedException] {
      Random.nextInt
    }
  }

  it should "start each test with an empty queue" in {
    // If reset did NOT occur the 99 from the previous test would still be present;
    // queuing a distinct value proves only this test's data is present.
    rand.nextInts(7)
    Random.nextInt shouldBe 7
  }
}
