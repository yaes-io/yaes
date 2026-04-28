package in.rcard.yaes.http.core

trait BodyEncoder[A] {
  def contentType: String
  def encode(value: A): String
}

object BodyEncoder {
  private val TextPlainUtf8: String = "text/plain; charset=UTF-8"

  given BodyEncoder[String] with {
    def contentType: String = TextPlainUtf8
    def encode(value: String): String = value
  }

  given BodyEncoder[Int] with {
    def contentType: String = TextPlainUtf8
    def encode(value: Int): String = value.toString
  }

  given BodyEncoder[Long] with {
    def contentType: String = TextPlainUtf8
    def encode(value: Long): String = value.toString
  }

  given BodyEncoder[Double] with {
    def contentType: String = TextPlainUtf8
    def encode(value: Double): String = value.toString
  }

  given BodyEncoder[Boolean] with {
    def contentType: String = TextPlainUtf8
    def encode(value: Boolean): String = value.toString
  }
}
