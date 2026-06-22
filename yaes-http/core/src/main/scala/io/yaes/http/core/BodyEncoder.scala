package io.yaes.http.core

/** Typeclass for encoding a value of type `A` into an HTTP message body.
  *
  * Instances must provide a content type string and a function to serialize
  * the value to a `String`. Custom encoders can be defined by implementing
  * this trait as a `given`.
  *
  * Example:
  * {{{
  * case class User(name: String, age: Int)
  *
  * given BodyEncoder[User] with {
  *   def contentType: String = "application/json"
  *   def encode(user: User): String =
  *     s"""{"name":"${user.name}","age":${user.age}}"""
  * }
  *
  * val response = Response.ok(User("Alice", 30))
  * }}}
  *
  * @tparam A the type of the value to encode
  */
trait BodyEncoder[A] {

  /** The MIME content type produced by this encoder (e.g. `"text/plain; charset=UTF-8"`). */
  def contentType: String

  /** Encodes `value` into its string representation for use as an HTTP message body.
    *
    * @param value the value to encode
    * @return the string representation of `value`
    */
  def encode(value: A): String
}

object BodyEncoder {
  private val TextPlainUtf8: String = "text/plain; charset=UTF-8"

  /** BodyEncoder for `String` values, content type `text/plain; charset=UTF-8`. */
  given BodyEncoder[String] with {
    def contentType: String = TextPlainUtf8
    def encode(value: String): String = value
  }

  /** BodyEncoder for `Int` values, content type `text/plain; charset=UTF-8`. */
  given BodyEncoder[Int] with {
    def contentType: String = TextPlainUtf8
    def encode(value: Int): String = value.toString
  }

  /** BodyEncoder for `Long` values, content type `text/plain; charset=UTF-8`. */
  given BodyEncoder[Long] with {
    def contentType: String = TextPlainUtf8
    def encode(value: Long): String = value.toString
  }

  /** BodyEncoder for `Double` values, content type `text/plain; charset=UTF-8`. */
  given BodyEncoder[Double] with {
    def contentType: String = TextPlainUtf8
    def encode(value: Double): String = value.toString
  }

  /** BodyEncoder for `Boolean` values, content type `text/plain; charset=UTF-8`. */
  given BodyEncoder[Boolean] with {
    def contentType: String = TextPlainUtf8
    def encode(value: Boolean): String = value.toString
  }
}
