package in.rcard.yaes

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.*

private enum CBState:
  case Closed(failures: Int)
  case Open(trippedAt: Duration)
  case HalfOpen

/** A circuit breaker that protects a downstream call by cycling through Closed, Open, and
  * Half-Open states based on consecutive `Raise[E]` failures.
  *
  * `CircuitBreaker` is not an effect — it is a stateful orchestrator. The protected block just
  * runs, succeeds, or fails; it never calls `CircuitBreaker` directly.
  *
  * State transitions:
  *   - Closed → Open: after `failureThreshold` consecutive matching failures
  *   - Open → Half-Open: lazily on the next call after `resetTimeout` elapses
  *   - Half-Open → Closed: when a probe succeeds
  *   - Half-Open → Open: when a probe fails (timer is reset)
  *
  * Example:
  * {{{
  * import in.rcard.yaes.*
  * import scala.concurrent.duration.*
  *
  * case class DbError(msg: String)
  *
  * given CircuitBreaker[DbError] =
  *   CircuitBreaker.make(CircuitBreaker.Config.consecutive(3, 5.seconds))
  *
  * val result: Either[CircuitBreaker.Open, Either[DbError, String]] = Clock.run {
  *   Raise.either[CircuitBreaker.Open, Either[DbError, String]] {
  *     Raise.either[DbError, String] {
  *       CircuitBreaker.protect[DbError] {
  *         findUser(42)
  *       }
  *     }
  *   }
  * }
  * }}}
  *
  * @tparam E
  *   the error type being tracked by this circuit breaker
  */
class CircuitBreaker[E] private (
    private val config: CircuitBreaker.Config[E],
    private val stateRef: AtomicReference[CBState]
)

object CircuitBreaker:

  /** Raised via `Raise[CircuitBreaker.Open]` when the circuit is in the Open state.
    *
    * @param resetAt
    *   wall-clock time at which the circuit may attempt a Half-Open probe
    */
  case class Open(resetAt: Instant)

  /** Configuration for a [[CircuitBreaker]].
    *
    * Create instances via [[Config.consecutive]]. Use [[failingWhen]] to restrict which errors
    * increment the failure counter.
    *
    * Example:
    * {{{
    * val config = CircuitBreaker.Config.consecutive[DbError](3, 5.seconds)
    *
    * // Only count connection errors, not auth errors
    * val selective = CircuitBreaker.Config.consecutive[AppError](3, 5.seconds)
    *   .failingWhen(_.isInstanceOf[ConnectionError])
    * }}}
    *
    * @param failureThreshold
    *   number of consecutive counted failures before opening the circuit (clamped to 1 if ≤ 0)
    * @param resetTimeout
    *   duration the circuit stays Open before allowing a Half-Open probe (clamped to
    *   `Duration.Zero` if non-positive)
    * @param isFailure
    *   predicate deciding whether a raised error increments the failure counter; defaults to
    *   `_ => true` (all errors count)
    * @tparam E
    *   the error type being tracked
    */
  final case class Config[E](
      failureThreshold: Int,
      resetTimeout: FiniteDuration,
      isFailure: E => Boolean
  ):
    /** Returns a copy of this config with the given failure predicate.
      *
      * Pass the full union type as `E` and discriminate via this predicate to avoid the
      * contravariance bypass described in ADR 0003.
      *
      * @param predicate
      *   function returning `true` for errors that should increment the failure counter
      * @return
      *   a new [[Config]] with the predicate applied
      */
    def failingWhen(predicate: E => Boolean): Config[E] = copy(isFailure = predicate)

  object Config:

    /** Creates a [[Config]] that trips after `failureThreshold` consecutive failures.
      *
      * Invalid inputs are clamped to sensible defaults:
      *   - `failureThreshold ≤ 0` becomes `1`
      *   - `resetTimeout ≤ Duration.Zero` becomes `Duration.Zero`
      *
      * Example:
      * {{{
      * val config = CircuitBreaker.Config.consecutive(3, 5.seconds)
      * }}}
      *
      * @param failureThreshold
      *   number of consecutive failures needed to open the circuit
      * @param resetTimeout
      *   how long the circuit stays Open before attempting a Half-Open probe
      * @tparam E
      *   the error type being tracked
      * @return
      *   a new [[Config]] with `isFailure = _ => true`
      */
    def consecutive[E](failureThreshold: Int, resetTimeout: FiniteDuration): Config[E] =
      val safeThreshold = failureThreshold max 1
      val safeTimeout   = if resetTimeout > Duration.Zero then resetTimeout else Duration.Zero
      Config(safeThreshold, safeTimeout, _ => true)

  /** Creates a new [[CircuitBreaker]] instance in the Closed state, backed by an
    * `AtomicReference` for thread-safe state transitions.
    *
    * Example:
    * {{{
    * given CircuitBreaker[DbError] =
    *   CircuitBreaker.make(CircuitBreaker.Config.consecutive(3, 5.seconds))
    * }}}
    *
    * @param config
    *   the circuit breaker configuration
    * @tparam E
    *   the error type being tracked
    * @return
    *   a new [[CircuitBreaker]] starting in Closed state with zero failure count
    */
  def make[E](config: Config[E]): CircuitBreaker[E] =
    new CircuitBreaker[E](config, AtomicReference(CBState.Closed(0)))

  /** Protects `block` using the `CircuitBreaker[E]` in implicit scope.
    *
    * Behavior by state:
    *   - **Closed**: executes the block; failures increment the counter; at `failureThreshold`
    *     consecutive failures the circuit opens.
    *   - **Open**: fast-fails via `Raise[CircuitBreaker.Open]` without executing the block.
    *     After `resetTimeout` elapses the next call transitions to Half-Open.
    *   - **Half-Open**: runs a probe; success closes the circuit, failure re-opens it.
    *
    * Only errors for which `Config.isFailure` returns `true` increment the counter. Non-matching
    * errors are re-raised immediately via `Raise[E]` without changing circuit state.
    *
    * Example:
    * {{{
    * given CircuitBreaker[DbError] =
    *   CircuitBreaker.make(CircuitBreaker.Config.consecutive(3, 5.seconds))
    *
    * val result = Clock.run {
    *   Raise.either[CircuitBreaker.Open, Either[DbError, String]] {
    *     Raise.either[DbError, String] {
    *       CircuitBreaker.protect[DbError] {
    *         findUser(42)
    *       }
    *     }
    *   }
    * }
    * }}}
    *
    * @tparam E
    *   the error type to track; must match the `CircuitBreaker[E]` in scope
    * @return
    *   a partially applied object; call `.apply(block)` or use as `CircuitBreaker.protect[E] { block }`
    */
  def protect[E]: ProtectPartiallyApplied[E] = new ProtectPartiallyApplied[E]

  /** Partially applied form of `protect` that pins the error type `E` before the block.
    *
    * @tparam E
    *   the error type being tracked
    */
  final class ProtectPartiallyApplied[E]:

    /** Runs `block` through the circuit breaker.
      *
      * @tparam A
      *   the result type of the block
      * @param block
      *   the computation to protect
      * @param cb
      *   the circuit breaker instance (resolved from implicit scope)
      * @param clock
      *   provides wall-clock and monotonic time
      * @param raise
      *   outer error channel for re-raising domain errors of type `E`
      * @param raiseOpen
      *   outer error channel for signalling that the circuit is Open
      * @return
      *   the result of the block if it succeeds
      */
    def apply[A](block: Raise[E] ?=> A)(using
        cb: CircuitBreaker[E],
        clock: Clock,
        raise: Raise[E],
        raiseOpen: Raise[CircuitBreaker.Open]
    ): A =
      val currentState = cb.stateRef.get()
      currentState match
        case CBState.Closed(_) | CBState.HalfOpen =>
          runBlock(cb, block)
        case open @ CBState.Open(trippedAt) =>
          val nowNanos     = clock.nowMonotonic.toNanos
          val trippedNanos = trippedAt.toNanos
          val timeoutNanos = cb.config.resetTimeout.toNanos
          if nowNanos - trippedNanos >= timeoutNanos then
            // Use `open` (the actual stored reference) so AtomicReference.compareAndSet succeeds
            cb.stateRef.compareAndSet(open, CBState.HalfOpen)
            runBlock(cb, block)
          else
            val remainingNanos = (timeoutNanos - (nowNanos - trippedNanos)) max 0L
            raiseOpen.raise(CircuitBreaker.Open(clock.now.plusNanos(remainingNanos)))

    private def runBlock[A](cb: CircuitBreaker[E], block: Raise[E] ?=> A)(using
        clock: Clock,
        raise: Raise[E],
        raiseOpen: Raise[CircuitBreaker.Open]
    ): A =
      val outcome = Raise.fold[E, A, Either[E, A]](block)(
        onError = error =>
          if cb.config.isFailure(error) then
            cb.stateRef.updateAndGet:
              case CBState.Closed(failures) =>
                val next = failures + 1
                if next >= cb.config.failureThreshold then CBState.Open(clock.nowMonotonic)
                else CBState.Closed(next)
              case CBState.HalfOpen           => CBState.Open(clock.nowMonotonic)
              case open @ CBState.Open(_)     => open
          Left(error)
      )(onSuccess = value =>
        cb.stateRef.updateAndGet:
          case CBState.Closed(_)          => CBState.Closed(0)
          case CBState.HalfOpen           => CBState.Closed(0)
          case open @ CBState.Open(_)     => open
        Right(value)
      )
      outcome match
        case Right(value) => value
        case Left(error)  => raise.raise(error)
