package in.rcard.yaes.http.client

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

trait PathParamEncoder[A]:
  def encode(value: A): String

object PathParamEncoder:
  given PathParamEncoder[String] with {
    def encode(v: String): String = v
  }
  given PathParamEncoder[Int] with {
    def encode(v: Int): String = v.toString
  }
  given PathParamEncoder[Long] with {
    def encode(v: Long): String = v.toString
  }
  given PathParamEncoder[Boolean] with {
    def encode(v: Boolean): String = v.toString
  }
  given PathParamEncoder[Double] with {
    def encode(v: Double): String = v.toString
  }
  given PathParamEncoder[UUID] with {
    def encode(v: UUID): String = v.toString
  }

/** Wrapper carrying a URL-encoded path segment value.
  *
  * Produced via implicit [[Conversion]] from any type that has a [[PathParamEncoder]].
  * Users never construct this directly — it appears transparently inside `uri"..."`.
  */
class UriParam(val encoded: String) extends AnyVal

object UriParam:
  given [A](using enc: PathParamEncoder[A]): Conversion[A, UriParam] =
    a => UriParam(URLEncoder.encode(enc.encode(a), UTF_8).replace("+", "%20"))
