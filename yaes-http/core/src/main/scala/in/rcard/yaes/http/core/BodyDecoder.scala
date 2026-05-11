package in.rcard.yaes.http.core

import in.rcard.yaes.*

/** Typeclass for decoding an HTTP message body string into a value of type `A`.
  *
  * Instances must provide a `decode` method that either returns the decoded value
  * or raises a `DecodingError` describing what went wrong.
  * Custom decoders can be defined by implementing this trait as a `given`.
  *
  * Example:
  * {{{
  * case class User(name: String, age: Int)
  *
  * given BodyDecoder[User] with {
  *   def decode(body: String): User raises DecodingError = {
  *     // JSON parsing logic
  *     val namePattern = """"name":"([^"]+)"""".r
  *     val agePattern  = """"age":(\d+)""".r
  *     val nameOpt = namePattern.findFirstMatchIn(body).map(_.group(1))
  *     val ageOpt  = agePattern.findFirstMatchIn(body).flatMap(m => m.group(1).toIntOption)
  *     (nameOpt, ageOpt) match {
  *       case (Some(name), Some(age)) => User(name, age)
  *       case _ => Raise.raise(DecodingError.ParseError(s"Invalid User JSON: $body"))
  *     }
  *   }
  * }
  *
  * // In handler
  * val user = request.as[User]  // Uses BodyDecoder[User]
  * }}}
  *
  * @tparam A the type to decode from the HTTP body string
  */
trait BodyDecoder[A] {

  /** Decodes `body` into a value of type `A`, raising a `DecodingError` on failure.
    *
    * @param body the raw HTTP message body string
    * @return the decoded value of type `A`
    */
  def decode(body: String): A raises DecodingError
}

object BodyDecoder {

  /** Built-in `BodyDecoder` for `String` values (identity decoder). */
  given BodyDecoder[String] with {
    def decode(body: String): String raises DecodingError = body
  }

  /** Built-in `BodyDecoder` for `Int` values.
    *
    * Raises `DecodingError.ParseError(...)` if `body` is not a valid integer.
    */
  given BodyDecoder[Int] with {
    def decode(body: String): Int raises DecodingError =
      body.toIntOption match {
        case Some(i) => i
        case None    => Raise.raise(DecodingError.ParseError(s"Invalid integer: $body"))
      }
  }

  /** Built-in `BodyDecoder` for `Long` values.
    *
    * Raises `DecodingError.ParseError(...)` if `body` is not a valid long integer.
    */
  given BodyDecoder[Long] with {
    def decode(body: String): Long raises DecodingError =
      body.toLongOption match {
        case Some(l) => l
        case None    => Raise.raise(DecodingError.ParseError(s"Invalid long: $body"))
      }
  }

  /** Built-in `BodyDecoder` for `Double` values.
    *
    * Raises `DecodingError.ParseError(...)` if `body` is not a valid double.
    */
  given BodyDecoder[Double] with {
    def decode(body: String): Double raises DecodingError =
      body.toDoubleOption match {
        case Some(d) => d
        case None    => Raise.raise(DecodingError.ParseError(s"Invalid double: $body"))
      }
  }

  /** Built-in `BodyDecoder` for `Boolean` values.
    *
    * Accepts `"true"` and `"false"` (case-insensitive).
    * Raises `DecodingError.ParseError(...)` for any other input.
    */
  given BodyDecoder[Boolean] with {
    def decode(body: String): Boolean raises DecodingError =
      body.toBooleanOption match {
        case Some(b) => b
        case None    => Raise.raise(DecodingError.ParseError(s"Invalid boolean: $body"))
      }
  }
}
