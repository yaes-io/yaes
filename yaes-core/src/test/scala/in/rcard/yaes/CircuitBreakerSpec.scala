package in.rcard.yaes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration.*
import java.time.Instant
import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}

class CircuitBreakerSpec extends AnyFlatSpec with Matchers {

  sealed trait AppError
  case class ConnectionError(msg: String) extends AppError
  case class AuthError(msg: String)       extends AppError

  class FakeClock(
      @volatile private var _monoNanos: Long = 0L,
      @volatile private var _wallTime: Instant = Instant.EPOCH
  ) extends Clock.Unsafe {
    def now: Instant           = _wallTime
    def nowMonotonic: Duration = _monoNanos.nanos
    def advance(d: FiniteDuration): Unit = {
      _monoNanos += d.toNanos
      _wallTime = _wallTime.plusNanos(d.toNanos)
    }
  }

  "CircuitBreaker" should "succeed immediately when block succeeds in Closed state" in {
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(3, 5.seconds))
    given CircuitBreaker[String] = cb
    val result = Clock.run {
      Raise.either[CircuitBreaker.Open, Either[String, Int]] {
        Raise.either[String, Int] {
          CircuitBreaker.protect[String] { 42 }
        }
      }
    }
    result shouldBe Right(Right(42))
  }

  it should "re-raise error via Raise[E] when block fails below threshold" in {
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(3, 5.seconds))
    given CircuitBreaker[String] = cb
    val result = Clock.run {
      Raise.either[CircuitBreaker.Open, Either[String, Int]] {
        Raise.either[String, Int] {
          CircuitBreaker.protect[String] { Raise.raise("error") }
        }
      }
    }
    result shouldBe Right(Left("error"))
  }

  it should "trip to Open after failureThreshold consecutive failures" in {
    val fakeClock = new FakeClock()
    given Clock = fakeClock
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(3, 5.seconds))
    given CircuitBreaker[String] = cb

    for _ <- 1 to 2 do
      Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
        Raise.either[String, Unit] {
          CircuitBreaker.protect[String] { Raise.raise("fail") }
        }
      }

    val trippingResult = Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
      Raise.either[String, Unit] {
        CircuitBreaker.protect[String] { Raise.raise("fail") }
      }
    }
    trippingResult shouldBe Right(Left("fail"))

    val result = Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
      Raise.either[String, Unit] {
        CircuitBreaker.protect[String] { () }
      }
    }
    result should matchPattern { case Left(_: CircuitBreaker.Open) => }
  }

  it should "not trip circuit with fewer than failureThreshold failures" in {
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(3, 5.seconds))
    given CircuitBreaker[String] = cb

    for _ <- 1 to 2 do
      Clock.run {
        Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
          Raise.either[String, Unit] {
            CircuitBreaker.protect[String] { Raise.raise("fail") }
          }
        }
      }

    val result = Clock.run {
      Raise.either[CircuitBreaker.Open, Either[String, Int]] {
        Raise.either[String, Int] {
          CircuitBreaker.protect[String] { 42 }
        }
      }
    }
    result shouldBe Right(Right(42))
  }

  it should "not execute block when circuit is Open (fast fail)" in {
    val fakeClock = new FakeClock()
    given Clock = fakeClock
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(2, 5.seconds))
    given CircuitBreaker[String] = cb
    var blockExecuted = false

    for _ <- 1 to 2 do
      Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
        Raise.either[String, Unit] {
          CircuitBreaker.protect[String] { Raise.raise("fail") }
        }
      }

    Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
      Raise.either[String, Unit] {
        CircuitBreaker.protect[String] {
          blockExecuted = true
          ()
        }
      }
    }

    blockExecuted shouldBe false
  }

  it should "transition from Open to Half-Open after resetTimeout elapses" in {
    val fakeClock = new FakeClock()
    given Clock = fakeClock
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(2, 5.seconds))
    given CircuitBreaker[String] = cb

    for _ <- 1 to 2 do
      Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
        Raise.either[String, Unit] {
          CircuitBreaker.protect[String] { Raise.raise("fail") }
        }
      }

    fakeClock.advance(5.seconds + 1.nano)

    var blockExecuted = false
    Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
      Raise.either[String, Unit] {
        CircuitBreaker.protect[String] {
          blockExecuted = true
          ()
        }
      }
    }
    blockExecuted shouldBe true
  }

  it should "close circuit after Half-Open probe succeeds" in {
    val fakeClock = new FakeClock()
    given Clock = fakeClock
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(2, 5.seconds))
    given CircuitBreaker[String] = cb

    for _ <- 1 to 2 do
      Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
        Raise.either[String, Unit] {
          CircuitBreaker.protect[String] { Raise.raise("fail") }
        }
      }

    fakeClock.advance(6.seconds)
    Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
      Raise.either[String, Unit] {
        CircuitBreaker.protect[String] { () }
      }
    }

    val result = Raise.either[CircuitBreaker.Open, Either[String, Int]] {
      Raise.either[String, Int] {
        CircuitBreaker.protect[String] { 42 }
      }
    }
    result shouldBe Right(Right(42))
  }

  it should "re-open circuit when Half-Open probe fails" in {
    val fakeClock = new FakeClock()
    given Clock = fakeClock
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(2, 5.seconds))
    given CircuitBreaker[String] = cb

    for _ <- 1 to 2 do
      Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
        Raise.either[String, Unit] {
          CircuitBreaker.protect[String] { Raise.raise("fail") }
        }
      }

    fakeClock.advance(6.seconds)
    Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
      Raise.either[String, Unit] {
        CircuitBreaker.protect[String] { Raise.raise("probe-fail") }
      }
    }

    var blockExecuted = false
    val result = Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
      Raise.either[String, Unit] {
        CircuitBreaker.protect[String] {
          blockExecuted = true
          ()
        }
      }
    }
    result should matchPattern { case Left(_: CircuitBreaker.Open) => }
    blockExecuted shouldBe false
  }

  it should "reset failure counter to zero after successful call in Closed state" in {
    val cb = CircuitBreaker.make[String](CircuitBreaker.Config.consecutive(3, 5.seconds))
    given CircuitBreaker[String] = cb

    for _ <- 1 to 2 do
      Clock.run {
        Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
          Raise.either[String, Unit] {
            CircuitBreaker.protect[String] { Raise.raise("fail") }
          }
        }
      }

    Clock.run {
      Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
        Raise.either[String, Unit] {
          CircuitBreaker.protect[String] { () }
        }
      }
    }

    for _ <- 1 to 2 do
      Clock.run {
        Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
          Raise.either[String, Unit] {
            CircuitBreaker.protect[String] { Raise.raise("fail") }
          }
        }
      }

    val result = Clock.run {
      Raise.either[CircuitBreaker.Open, Either[String, Int]] {
        Raise.either[String, Int] {
          CircuitBreaker.protect[String] { 42 }
        }
      }
    }
    result shouldBe Right(Right(42))
  }

  it should "not count errors where isFailure predicate returns false" in {
    val config = CircuitBreaker.Config.consecutive[AppError](3, 5.seconds)
      .failingWhen(_.isInstanceOf[ConnectionError])
    val cb = CircuitBreaker.make[AppError](config)
    given CircuitBreaker[AppError] = cb

    for _ <- 1 to 5 do
      Clock.run {
        Raise.either[CircuitBreaker.Open, Either[AppError, Unit]] {
          Raise.either[AppError, Unit] {
            CircuitBreaker.protect[AppError] { Raise.raise(AuthError("auth fail")) }
          }
        }
      }

    val result = Clock.run {
      Raise.either[CircuitBreaker.Open, Either[AppError, Int]] {
        Raise.either[AppError, Int] {
          CircuitBreaker.protect[AppError] { 42 }
        }
      }
    }
    result shouldBe Right(Right(42))
  }

  it should "trip circuit counting only errors matching isFailure predicate" in {
    val fakeClock = new FakeClock()
    given Clock = fakeClock
    val config = CircuitBreaker.Config.consecutive[AppError](3, 5.seconds)
      .failingWhen(_.isInstanceOf[ConnectionError])
    val cb = CircuitBreaker.make[AppError](config)
    given CircuitBreaker[AppError] = cb

    for _ <- 1 to 3 do
      Raise.either[CircuitBreaker.Open, Either[AppError, Unit]] {
        Raise.either[AppError, Unit] {
          CircuitBreaker.protect[AppError] { Raise.raise(ConnectionError("timeout")) }
        }
      }

    var blockExecuted = false
    val result = Raise.either[CircuitBreaker.Open, Either[AppError, Unit]] {
      Raise.either[AppError, Unit] {
        CircuitBreaker.protect[AppError] {
          blockExecuted = true
          ()
        }
      }
    }
    result should matchPattern { case Left(_: CircuitBreaker.Open) => }
    blockExecuted shouldBe false
  }

  it should "count failures when error is raised with widened union type" in {
    val fakeClock = new FakeClock()
    given Clock = fakeClock
    val config = CircuitBreaker.Config.consecutive[AppError](2, 5.seconds)
    val cb = CircuitBreaker.make[AppError](config)
    given CircuitBreaker[AppError] = cb

    var attempts = 0
    for _ <- 1 to 2 do
      Raise.either[CircuitBreaker.Open, Either[AppError, Unit]] {
        Raise.either[AppError, Unit] {
          CircuitBreaker.protect[AppError] {
            attempts += 1
            Raise.raise(ConnectionError("fail"): AppError)
          }
        }
      }

    val result = Raise.either[CircuitBreaker.Open, Either[AppError, Unit]] {
      Raise.either[AppError, Unit] {
        CircuitBreaker.protect[AppError] { () }
      }
    }
    result should matchPattern { case Left(_: CircuitBreaker.Open) => }
    attempts shouldBe 2
  }

  it should "handle concurrent failing calls correctly (thread-safety)" in {
    val fakeClock = new FakeClock()
    given Clock = fakeClock
    val numThreads = 20
    val threshold  = numThreads
    val config = CircuitBreaker.Config.consecutive[String](threshold, 5.seconds)
    val cb = CircuitBreaker.make[String](config)
    given CircuitBreaker[String] = cb
    val executor   = Executors.newFixedThreadPool(numThreads)
    val startLatch = new CountDownLatch(1)

    val tasks = (1 to numThreads).map { _ =>
      new Runnable {
        def run(): Unit = {
          startLatch.await()
          Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
            Raise.either[String, Unit] {
              CircuitBreaker.protect[String] { Raise.raise("fail") }
            }
          }
          ()
        }
      }
    }

    tasks.foreach(executor.submit)
    startLatch.countDown()
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)

    val result = Raise.either[CircuitBreaker.Open, Either[String, Unit]] {
      Raise.either[String, Unit] {
        CircuitBreaker.protect[String] { () }
      }
    }
    result should matchPattern { case Left(_: CircuitBreaker.Open) => }
  }
}
