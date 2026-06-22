package io.yaes

/** The Reader effect type, representing a computation that can read a value of type `R`.
  *
  * @tparam R
  *   the type of the environment value
  */
type Reader[R] = Reader.Unsafe[R]

/** Infix type alias for Reader effect. `A reads R` is equivalent to `Reader[R] ?=> A`.
  *
  * @tparam A
  *   the result type of the computation
  * @tparam R
  *   the type of the environment value
  *
  * @example
  * {{{
  * def getRetries: Int reads Config =
  *   Reader.read[Config].maxRetries
  * }}}
  */
infix type reads[A, R] = Reader[R] ?=> A

/** Reader effect for read-only access to environment values.
  *
  * The Reader effect allows computations to access a shared environment value of type `R` in a
  * purely functional manner. The value is immutable within a scope and can be temporarily overridden
  * via [[local]].
  *
  * @example
  * {{{
  * case class Config(maxRetries: Int, timeout: Int)
  *
  * val result = Reader.run(Config(3, 5000)) {
  *   val retries = Reader.read[Config].maxRetries // 3
  *   val modified = Reader.local(_.copy(maxRetries = 10)) {
  *     Reader.read[Config].maxRetries // 10
  *   }
  *   val restored = Reader.read[Config].maxRetries // 3
  *   (retries, modified, restored)
  * }
  * // result = (3, 10, 3)
  * }}}
  */
object Reader {

  /** Wraps a raw value into a [[Reader]] context.
    *
    * @tparam R
    *   the type of the environment value
    * @param value
    *   the raw value to wrap
    * @return
    *   a [[Reader]] holding the given value
    */
  inline def reader[R](value: R): Reader[R] = value

  /** Reads the current environment value.
    *
    * @tparam R
    *   the type of the environment value
    * @param reader
    *   the Reader effect context
    * @return
    *   the current environment value
    *
    * @example
    * {{{
    * Reader.run(42) {
    *   val value = Reader.read[Int] // 42
    * }
    * }}}
    */
  inline def read[R](using reader: Reader[R]): R = reader

  /** Runs a block with a modified environment value. The function `f` transforms the current
    * environment (possibly changing its type) to produce a new one for the inner block. The
    * original value is restored after the block completes.
    *
    * Thread-safe by construction: the inner block receives its own given value; no shared mutable
    * state exists.
    *
    * @tparam R1
    *   the type of the outer environment value
    * @tparam R2
    *   the type of the inner environment value
    * @tparam A
    *   the result type of the block
    * @param f
    *   the function to transform the current environment value
    * @param block
    *   the computation to execute with the modified environment
    * @param reader
    *   the Reader effect context
    * @return
    *   the result of the block
    *
    * @example
    * {{{
    * Reader.run(Config(3, 5000)) {
    *   Reader.local(_.copy(maxRetries = 10)) {
    *     Reader.read[Config].maxRetries // 10
    *   }
    *   // maxRetries is 3 again here
    * }
    * }}}
    */
  inline def local[R1, R2, A](inline f: R1 => R2)(inline block: Reader[R2] ?=> A)(using
      reader: Reader[R1]
  ): A =
    run(f(reader))(block)

  /** Runs a block with a modified environment value of the same type. Delegates to the general
    * [[local]] overload. Provided for ergonomics when the environment type does not change.
    *
    * @tparam R
    *   the type of the environment value
    * @tparam A
    *   the result type of the block
    * @param f
    *   the function to transform the current environment value
    * @param block
    *   the computation to execute with the modified environment
    * @param reader
    *   the Reader effect context
    * @return
    *   the result of the block
    *
    * @example
    * {{{
    * Reader.run(42) {
    *   Reader.local(_ + 10) {
    *     Reader.read[Int] // 52
    *   }
    * }
    * }}}
    */
  inline def local[R, A](inline f: R => R)(inline block: Reader[R] ?=> A)(using
      reader: Reader[R]
  ): A =
    local[R, R, A](f)(block)

  /** Runs a computation with the Reader effect, providing a value to be read.
    *
    * Returns `A` directly (not a tuple), since the environment value is immutable.
    *
    * @tparam R
    *   the type of the environment value
    * @tparam A
    *   the result type of the computation
    * @param value
    *   the environment value to provide
    * @param block
    *   the computation to execute with access to the environment value
    * @return
    *   the result of the computation
    *
    * @example
    * {{{
    * val result = Reader.run(42) {
    *   Reader.read[Int] * 2
    * }
    * // result = 84
    * }}}
    */
  inline def run[R, A](value: R)(inline block: Reader[R] ?=> A): A =
    block(using reader(value))

  /** Opaque type alias backing the Reader effect. Erases to `R` at runtime with zero overhead.
    *
    * @tparam R
    *   the type of the environment value
    */
  opaque type Unsafe[R] = R
}
