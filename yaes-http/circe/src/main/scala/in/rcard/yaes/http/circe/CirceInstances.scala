package in.rcard.yaes.http.circe

import in.rcard.yaes.*
import in.rcard.yaes.http.core.{BodyDecoder, BodyEncoder, DecodingError}
import io.circe.{Decoder, DecodingFailure, Encoder, ParsingFailure}
import io.circe.parser.decodeAccumulating as circeDecodeAccumulating
import io.circe.syntax.*
import cats.data.Validated

/** Default `BodyEncoder` instance for any type `A` that has a Circe [[io.circe.Encoder]] in scope.
  *
  * Encodes values of type `A` as compact JSON using Circe's `asJson.noSpaces` and sets the HTTP
  * `Content-Type` header to `application/json`.
  *
  * Example:
  * {{{
  *   import io.circe.Encoder
  *   import in.rcard.yaes.http.circe.given
  *
  *   final case class MyPayload(value: String) derives Encoder.AsObject
  *
  *   // `circeBodyEncoder` provides an implicit BodyEncoder[MyPayload]
  *   val encoded: String = summon[BodyEncoder[MyPayload]].encode(MyPayload("hello"))
  *   // encoded: """{"value":"hello"}"""
  * }}}
  *
  * @tparam A the type of the value to encode
  */
given circeBodyEncoder[A](using encoder: Encoder[A]): BodyEncoder[A] with {
  def contentType: String = "application/json"
  def encode(value: A): String = value.asJson.noSpaces
}

/** Default `BodyDecoder` instance for any type `A` that has a Circe [[io.circe.Decoder]] in scope.
  *
  * Decodes JSON bodies using Circe's `decodeAccumulating`, collecting all failures into a
  * non-empty `List[DecodingError]`:
  *   - A [[io.circe.ParsingFailure]] (invalid JSON syntax) is mapped to
  *     [[in.rcard.yaes.http.core.DecodingError.ParseError]] with the original message and
  *     underlying exception attached.
  *   - Each [[io.circe.DecodingFailure]] (valid JSON but wrong shape, e.g. missing or mistyped
  *     fields) is mapped to [[in.rcard.yaes.http.core.DecodingError.ValidationError]] with the
  *     original message.
  *
  * Example:
  * {{{
  *   import io.circe.Decoder
  *   import in.rcard.yaes.http.circe.given
  *   import in.rcard.yaes.Raise
  *   import in.rcard.yaes.http.core.DecodingError
  *
  *   final case class MyPayload(value: String) derives Decoder
  *
  *   // `circeBodyDecoder` provides an implicit BodyDecoder[MyPayload]
  *   val result = Raise.either[List[DecodingError], MyPayload] {
  *     summon[BodyDecoder[MyPayload]].decode("""{"value":"hello"}""")
  *   }
  *   // result: Right(MyPayload("hello"))
  * }}}
  *
  * @tparam A the type to decode from the HTTP body string
  */
given circeBodyDecoder[A](using decoder: Decoder[A]): BodyDecoder[A] with {
  def decode(body: String): A raises List[DecodingError] =
    circeDecodeAccumulating[A](body) match {
      case Validated.Valid(a) => a
      case Validated.Invalid(errs) =>
        Raise.raise(errs.toList.map {
          case pf: ParsingFailure  => DecodingError.ParseError(pf.getMessage, Option(pf.underlying))
          case df: DecodingFailure => DecodingError.ValidationError(df.getMessage)
        })
    }
}
