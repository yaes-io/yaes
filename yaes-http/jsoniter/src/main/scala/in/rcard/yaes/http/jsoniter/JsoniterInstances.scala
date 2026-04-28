package in.rcard.yaes.http.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import in.rcard.yaes.*
import in.rcard.yaes.http.core.{BodyDecoder, BodyEncoder, DecodingError}

given jsoniterBodyEncoder[A](using codec: JsonValueCodec[A]): BodyEncoder[A] with {
  def contentType: String = "application/json"
  def encode(value: A): String = writeToString(value)
}

given jsoniterBodyDecoder[A](using codec: JsonValueCodec[A]): BodyDecoder[A] with {
  def decode(body: String): A raises List[DecodingError] =
    try readFromString[A](body)
    catch case e: JsonReaderException =>
      Raise.raise(List(DecodingError.ParseError(e.getMessage, Option(e))))
}
