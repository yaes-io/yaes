package in.rcard.yaes.http.client

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

/** Typeclass that converts a value of type [[A]] to its string representation
  * for use as a URI path segment.
  *
  * URL-encoding of the resulting string is performed by [[UriParam]] — implementations
  * should return the raw string representation, not a percent-encoded value.
  *
  * Built-in instances exist for [[String]], [[Int]], [[Long]], [[Boolean]], [[Double]],
  * and [[java.util.UUID]].
  *
  * Example:
  * {{{
  * case class UserId(value: Long)
  *
  * given PathParamStringifier[UserId] with {
  *   def encode(value: UserId): String = value.value.toString
  * }
  * }}}
  *
  * @tparam A the type to convert to a path segment string
  */
trait PathParamStringifier[A]:
  def encode(value: A): String

object PathParamStringifier:
  given PathParamStringifier[String] with {
    def encode(value: String): String = value
  }
  given PathParamStringifier[Int] with {
    def encode(value: Int): String = value.toString
  }
  given PathParamStringifier[Long] with {
    def encode(value: Long): String = value.toString
  }
  given PathParamStringifier[Boolean] with {
    def encode(value: Boolean): String = value.toString
  }
  given PathParamStringifier[Double] with {
    def encode(value: Double): String = value.toString
  }
  given PathParamStringifier[UUID] with {
    def encode(value: UUID): String = value.toString
  }

/** Wrapper carrying a URL-encoded path segment value.
  *
  * Produced via implicit [[Conversion]] from any type that has a [[PathParamStringifier]].
  * Users never construct this directly — it appears transparently inside `uri"..."`.
  */
class UriParam(val encoded: String) extends AnyVal

object UriParam:
  given [A](using enc: PathParamStringifier[A]): Conversion[A, UriParam] =
    a => UriParam(URLEncoder.encode(enc.encode(a), UTF_8).replace("+", "%20"))
