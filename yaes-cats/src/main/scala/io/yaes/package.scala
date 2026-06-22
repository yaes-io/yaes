package io.yaes

import _root_.cats.data.NonEmptyChain
import _root_.cats.data.NonEmptyList as CatsNonEmptyList

/** YAES (Yet Another Effect System) - Core package.
  *
  * This package provides type aliases for the Cats Effect integration.
  */

/** Type alias for `cats.effect.IO[A]`, wrapping Cats Effect IO within the yaes effect system.
  *
  * @tparam A the type of the value produced by the IO computation
  */
type CatsIO[A] = _root_.cats.effect.IO[A]

/** Type alias for `Raise[cats.data.NonEmptyList[E]]`.
  *
  * Provides a convenient shorthand for error accumulation with `NonEmptyList`,
  * ensuring at least one error is present when raised.
  *
  * Example:
  * {{{
  * import cats.data.{NonEmptyList => CatsNonEmptyList}
  * import io.yaes.{Raise, RaiseNel}
  * import io.yaes.Raise.accumulating
  * import io.yaes.instances.accumulate.given
  *
  * def validatePositive(n: Int)(using Raise[String]): Int =
  *   if (n > 0) n else Raise.raise(s"$n is not positive")
  *
  * val result: RaiseNel[String] ?=> (Int, Int) =
  *   Raise.accumulate[CatsNonEmptyList, String, (Int, Int)] {
  *     val a = accumulating { validatePositive(-1) }
  *     val b = accumulating { validatePositive(-2) }
  *     (a, b)
  *   }
  * }}}
  */
type RaiseNel[E] = Raise[CatsNonEmptyList[E]]

/** Type alias for `Raise[NonEmptyChain[E]]`.
  *
  * Provides a convenient shorthand for error accumulation with `NonEmptyChain`,
  * ensuring at least one error is present when raised.
  *
  * Example:
  * {{{
  * import io.yaes.{Raise, RaiseNec}
  * import io.yaes.Raise.accumulating
  * import io.yaes.instances.accumulate.given
  *
  * def validatePositive(n: Int)(using Raise[String]): Int =
  *   if (n > 0) n else Raise.raise(s"$n is not positive")
  *
  * val result: RaiseNec[String] ?=> List[Int] =
  *   Raise.accumulate[NonEmptyChain, String, List[Int]] {
  *     List(1, -2, 3).map { n =>
  *       accumulating { validatePositive(n) }
  *     }
  *   }
  * }}}
  */
type RaiseNec[E] = Raise[NonEmptyChain[E]]
