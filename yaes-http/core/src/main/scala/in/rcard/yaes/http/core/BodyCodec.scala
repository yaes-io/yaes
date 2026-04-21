package in.rcard.yaes.http.core


import in.rcard.yaes.*
/** Typeclass for encoding/decoding HTTP body content.
  *
  * Codecs are string-based and self-describing (include Content-Type).
  * Use context parameters for automatic resolution in handlers.
  *
  * Example:
  * {{{
  * case class User(name: String, age: Int)
  *
  * given BodyCodec[User] with {
  *   def contentType: String = "application/json"
  *   def encode(user: User): String = s"""{"name":"${user.name}","age":${user.age}}"""
  *   def decode(body: String): User raises List[DecodingError] = {
  *     // JSON parsing logic
  *     ???
  *   }
  * }
  *
  * // In handler
  * val user = request.as[User]  // Uses BodyCodec[User]
  * Response.ok(user)             // Uses BodyCodec[User]
  * }}}
  *
  * @tparam A The type to encode/decode
  */
trait BodyCodec[A] {
  /** The Content-Type header value for this codec (e.g., "application/json") */
  def contentType: String

  /** Encode a value to a string representation */
  def encode(value: A): String

  /** Decode a string body to a value, raising a non-empty `List[DecodingError]` on failure. The list aggregates every error found during decoding; implementations MUST NOT raise an empty list. */
  def decode(body: String): A raises List[DecodingError]
}

object BodyCodec {
  /** Built-in codec for String (text/plain) */
  given BodyCodec[String] with {
    def contentType: String = "text/plain; charset=UTF-8"
    def encode(value: String): String = value
    def decode(body: String): String raises List[DecodingError] = body
  }

  /** Built-in codec for Int (text/plain) */
  given BodyCodec[Int] with {
    def contentType: String = "text/plain; charset=UTF-8"

    def encode(value: Int): String = value.toString

    def decode(body: String): Int raises List[DecodingError] =
      body.toIntOption match {
        case Some(i) => i
        case None =>
          Raise.raise(List(DecodingError.ParseError(s"Invalid integer: $body")))
      }
  }

  /** Built-in codec for Long (text/plain) */
  given BodyCodec[Long] with {
    def contentType: String = "text/plain; charset=UTF-8"

    def encode(value: Long): String = value.toString

    def decode(body: String): Long raises List[DecodingError] =
      body.toLongOption match {
        case Some(l) => l
        case None =>
          Raise.raise(List(DecodingError.ParseError(s"Invalid long: $body")))
      }
  }

  /** Built-in codec for Double (text/plain) */
  given BodyCodec[Double] with {
    def contentType: String = "text/plain; charset=UTF-8"

    def encode(value: Double): String = value.toString

    def decode(body: String): Double raises List[DecodingError] =
      body.toDoubleOption match {
        case Some(d) => d
        case None =>
          Raise.raise(List(DecodingError.ParseError(s"Invalid double: $body")))
      }
  }

  /** Built-in codec for Boolean (text/plain) */
  given BodyCodec[Boolean] with {
    def contentType: String = "text/plain; charset=UTF-8"

    def encode(value: Boolean): String = value.toString

    def decode(body: String): Boolean raises List[DecodingError] =
      body.toBooleanOption match {
        case Some(b) => b
        case None =>
          Raise.raise(List(DecodingError.ParseError(s"Invalid boolean: $body")))
      }
  }
}
