package io.yaes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch

import io.yaes.Async.*

class AsyncUnsupervisedSpec extends AnyFlatSpec with Matchers {

  "The Async.unsupervised scope" should "return promptly and cancel a still-running unjoined fiber" in {
    val started   = new CountDownLatch(1)
    val cancelled = new CountDownLatch(1)

    val result = Async.run {
      Async.unsupervised {
        Async.fork {
          try {
            started.countDown()
            Async.delay(10.seconds)
          } catch {
            case _: InterruptedException => cancelled.countDown()
          }
        }
        // Wait until the fiber is actually running before leaving the block.
        started.await(5, java.util.concurrent.TimeUnit.SECONDS) shouldBe true
      }
    }

    result shouldBe 42
    // The block returned without waiting for the 10-second delay, and the fiber was cancelled.
    cancelled.await(5, java.util.concurrent.TimeUnit.SECONDS) shouldBe true
  }

  it should "propagate an exception thrown from the main body of the block" in {
    val thrown = intercept[RuntimeException] {
      Async.run {
        Async.unsupervised {
          Async.fork {
            Async.delay(10.seconds)
          }
          throw new RuntimeException("boom")
        }
      }
    }

    thrown.getMessage shouldBe "boom"
  }

  it should "not fail the scope when an unjoined forked fiber fails" in {
    val failed = new CountDownLatch(1)

    val result = Async.run {
      Async.unsupervised {
        Async.fork {
          try throw new RuntimeException("fiber boom")
          finally failed.countDown()
        }
        failed.await(5, java.util.concurrent.TimeUnit.SECONDS) shouldBe true
        123
      }
    }

    result shouldBe 123
  }

  it should "run Async.fork inside the scope without modification and return the block result" in {
    val queue = new ConcurrentLinkedQueue[String]()

    val result = Async.run {
      Async.unsupervised {
        val fiber = Async.fork {
          queue.add("forked")
        }
        fiber.join()
        queue.add("main")
        "done"
      }
    }

    result shouldBe "done"
    queue.toArray should contain theSameElementsAs List("forked", "main")
  }
}
