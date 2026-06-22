package io.yaes.test.scalatest

import io.yaes.Random
import org.scalatest.{Outcome, TestSuite}
import org.scalatest.exceptions.TestFailedException

import scala.collection.mutable

/** A queue-based stub for the [[Random]] effect that lets tests supply deterministic values.
  *
  * Enqueue expected values before the code under test runs, then verify the results. If the code
  * under test consumes more values than were enqueued for a given type, a [[TestFailedException]]
  * is thrown immediately, failing the test with a descriptive message.
  *
  * Example:
  * {{{
  * val stub = new RandomStub()
  * stub.nextInts(42, 7)
  * stub.nextBooleans(true)
  *
  * given Random = stub
  * Random.nextInt    // 42
  * Random.nextInt    // 7
  * Random.nextBoolean // true
  * Random.nextInt    // throws TestFailedException: "RandomStub: no ints queued"
  * }}}
  */
class RandomStub extends Random.Unsafe {
  private val ints     = mutable.Queue[Int]()
  private val longs    = mutable.Queue[Long]()
  private val booleans = mutable.Queue[Boolean]()
  private val doubles  = mutable.Queue[Double]()

  /** Enqueues integer values to be returned by successive [[nextInt]] calls.
    *
    * @param values
    *   The integer values to enqueue, in the order they will be consumed.
    */
  def nextInts(values: Int*): Unit = ints.enqueueAll(values)

  /** Enqueues long values to be returned by successive [[nextLong]] calls.
    *
    * @param values
    *   The long values to enqueue, in the order they will be consumed.
    */
  def nextLongs(values: Long*): Unit = longs.enqueueAll(values)

  /** Enqueues boolean values to be returned by successive [[nextBoolean]] calls.
    *
    * @param values
    *   The boolean values to enqueue, in the order they will be consumed.
    */
  def nextBooleans(values: Boolean*): Unit = booleans.enqueueAll(values)

  /** Enqueues double values to be returned by successive [[nextDouble]] calls.
    *
    * @param values
    *   The double values to enqueue, in the order they will be consumed.
    */
  def nextDoubles(values: Double*): Unit = doubles.enqueueAll(values)

  /** Clears all queued values for all types.
    *
    * Normally called automatically by [[RandomSpec.withFixture]] between tests. Call this manually
    * if you need to reset state within a single test.
    */
  def reset(): Unit = {
    ints.clear()
    longs.clear()
    booleans.clear()
    doubles.clear()
  }

  /** Returns the next queued integer, or fails the test if no integers are queued.
    *
    * @return
    *   The next integer from the queue.
    * @throws TestFailedException
    *   if the integer queue is empty.
    */
  override def nextInt(): Int =
    if ints.isEmpty then throw new TestFailedException("RandomStub: no ints queued", 1)
    else ints.dequeue()

  /** Returns the next queued long, or fails the test if no longs are queued.
    *
    * @return
    *   The next long from the queue.
    * @throws TestFailedException
    *   if the long queue is empty.
    */
  override def nextLong(): Long =
    if longs.isEmpty then throw new TestFailedException("RandomStub: no longs queued", 1)
    else longs.dequeue()

  /** Returns the next queued boolean, or fails the test if no booleans are queued.
    *
    * @return
    *   The next boolean from the queue.
    * @throws TestFailedException
    *   if the boolean queue is empty.
    */
  override def nextBoolean(): Boolean =
    if booleans.isEmpty then throw new TestFailedException("RandomStub: no booleans queued", 1)
    else booleans.dequeue()

  /** Returns the next queued double, or fails the test if no doubles are queued.
    *
    * @return
    *   The next double from the queue.
    * @throws TestFailedException
    *   if the double queue is empty.
    */
  override def nextDouble(): Double =
    if doubles.isEmpty then throw new TestFailedException("RandomStub: no doubles queued", 1)
    else doubles.dequeue()
}

/** Mixin trait providing a shared [[RandomStub]] and automatic reset between tests.
  *
  * Mix this trait into a ScalaTest spec class to get a `given Random` backed by a [[RandomStub]].
  * The stub is automatically reset before each test via the stackable `withFixture` override, so
  * values enqueued in one test cannot leak into the next.
  *
  * '''Note:''' This trait is not safe for concurrent test execution. The shared [[RandomStub]]
  * uses mutable queues, and concurrent queue operations or resets will race if tests run in
  * parallel (e.g., when mixing in `ParallelTestExecution`). Use only with sequential test suites.
  *
  * Example:
  * {{{
  * class MySpec extends AnyFlatSpec with Matchers with RandomSpec {
  *
  *   "myFunction" should "use a queued int" in {
  *     rand.nextInts(42)
  *     val result = Random.nextInt
  *     result shouldBe 42
  *   }
  *
  *   it should "fail when no value is queued" in {
  *     intercept[TestFailedException] {
  *       Random.nextInt
  *     }
  *   }
  * }
  * }}}
  */
trait RandomSpec extends TestSuite {

  /** The shared [[RandomStub]] instance. Enqueue values on this before calling code under test. */
  val rand: RandomStub = new RandomStub()

  /** The [[Random]] given instance backed by [[rand]]. */
  given Random = rand

  /** Resets the [[RandomStub]] before each test and delegates to the next `withFixture` in the
    * mixin stack.
    *
    * @param test
    *   The test to run.
    * @return
    *   The outcome of the test.
    */
  abstract override def withFixture(test: NoArgTest): Outcome = {
    rand.reset()
    super.withFixture(test)
  }
}
