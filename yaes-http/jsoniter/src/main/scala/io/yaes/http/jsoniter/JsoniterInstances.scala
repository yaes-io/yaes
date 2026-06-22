package io.yaes.http.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.*
import io.yaes.*
import io.yaes.http.core.{BodyDecoder, BodyEncoder, DecodingError}

/** [[BodyEncoder]] for any type `A` with a jsoniter-scala [[JsonValueCodec]] in scope.
  *
  * Serializes values to compact JSON using [[writeToString]] and sets the
  * `Content-Type` to `application/json`.
  *
  * Example:
  * {{{
  * import com.github.plokhotnyuk.jsoniter_scala.core.*
  * import com.github.plokhotnyuk.jsoniter_scala.macros.*
  * import io.yaes.http.jsoniter.given
  *
  * case class User(name: String, age: Int)
  * given JsonValueCodec[User] = JsonCodecMaker.make
  *
  * val enc = summon[BodyEncoder[User]]
  * enc.encode(User("Alice", 30)) // {"name":"Alice","age":30}
  * }}}
  *
  * @tparam A the type to encode; requires a [[JsonValueCodec]] in scope
  */
given jsoniterBodyEncoder[A](using codec: JsonValueCodec[A]): BodyEncoder[A] with {
  def contentType: String = "application/json"
  def encode(value: A): String = writeToString(value)
}

/** [[BodyDecoder]] for any type `A` with a jsoniter-scala [[JsonValueCodec]] in scope.
  *
  * Parses JSON using [[readFromString]]. Any [[JsonReaderException]] (both syntax
  * and structural errors) is mapped to `DecodingError.ParseError(..., cause)`.
  *
  * Example:
  * {{{
  * import com.github.plokhotnyuk.jsoniter_scala.core.*
  * import com.github.plokhotnyuk.jsoniter_scala.macros.*
  * import io.yaes.http.jsoniter.given
  *
  * case class User(name: String, age: Int)
  * given JsonValueCodec[User] = JsonCodecMaker.make
  *
  * val dec = summon[BodyDecoder[User]]
  * // dec.decode(body) raises DecodingError on failure
  * }}}
  *
  * @tparam A the type to decode; requires a [[JsonValueCodec]] in scope
  */
given jsoniterBodyDecoder[A](using codec: JsonValueCodec[A]): BodyDecoder[A] with {
  def decode(body: String): A raises DecodingError =
    try readFromString[A](body)
    catch case e: JsonReaderException =>
      Raise.raise(DecodingError.ParseError(e.getMessage, Option(e)))
}
