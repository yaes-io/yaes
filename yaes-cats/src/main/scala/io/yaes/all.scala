package io.yaes

/** Unified imports for all YAES Cats integrations.
  *
  * For production code, prefer granular imports:
  * {{{
  * import io.yaes.interop.catseffect.*
  * import io.yaes.syntax.catseffect.given
  * import io.yaes.instances.raise.given
  * }}}
  *
  * This object is provided for convenience and quick exploration.
  *
  * Example:
  * {{{
  * import io.yaes.all.{given, *}
  *
  * // Now have access to all methods and syntax
  * val catsIO = catseffect.blockingIO(yaesProgram)
  * val v = validated.validated { Raise.raise("error") }
  * }}}
  */
object all {
  // Re-export all object methods
  export interop.catseffect
  export cats.validated
  export cats.accumulate

  // Re-export all syntax (for .given imports)
  export syntax.all.*

  // Re-export instances (for .given imports)
  export instances.raise.given
  export instances.accumulate.given
}
