package in.rcard.yaes.http.jsoniter.test

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import in.rcard.yaes.*
import in.rcard.yaes.http.core.{BodyDecoder, BodyEncoder, DecodingError}
import in.rcard.yaes.http.jsoniter.given
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues

class JsoniterInstancesSpec extends AnyFlatSpec with Matchers with EitherValues {

  case class User(name: String, age: Int)
  given JsonValueCodec[User] = JsonCodecMaker.make

  case class Address(street: String, city: String)
  case class Person(name: String, address: Address)
  given JsonValueCodec[Address] = JsonCodecMaker.make
  given JsonValueCodec[Person]  = JsonCodecMaker.make

  "jsoniterBodyEncoder" should "encode a case class to compact JSON" in {
    val enc = summon[BodyEncoder[User]]
    enc.encode(User("Alice", 30)) shouldBe """{"name":"Alice","age":30}"""
  }

  it should "have content type application/json" in {
    val enc = summon[BodyEncoder[User]]
    enc.contentType shouldBe "application/json"
  }

  it should "work with nested case classes" in {
    val enc = summon[BodyEncoder[Person]]
    enc.encode(Person("Alice", Address("123 Main St", "Springfield"))) shouldBe
      """{"name":"Alice","address":{"street":"123 Main St","city":"Springfield"}}"""
  }

  it should "resolve without a BodyDecoder import" in {
    assertCompiles("""
      import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
      import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
      import in.rcard.yaes.http.core.BodyEncoder
      import in.rcard.yaes.http.jsoniter.given

      case class EncodeOnly(value: String)
      given JsonValueCodec[EncodeOnly] = JsonCodecMaker.make

      summon[BodyEncoder[EncodeOnly]]
    """)
  }

  "jsoniterBodyDecoder" should "decode valid JSON to a case class" in {
    val dec    = summon[BodyDecoder[User]]
    val result = Raise.either { dec.decode("""{"name":"Bob","age":25}""") }
    result shouldBe Right(User("Bob", 25))
  }

  it should "raise ParseError for malformed JSON" in {
    val dec    = summon[BodyDecoder[User]]
    val result = Raise.either[List[DecodingError], User] { dec.decode("not json at all") }
    val errors = result.left.value
    errors.head shouldBe a[DecodingError.ParseError]
    errors.head.asInstanceOf[DecodingError.ParseError].cause shouldBe defined
  }

  it should "raise ParseError for empty input" in {
    val dec    = summon[BodyDecoder[User]]
    val result = Raise.either[List[DecodingError], User] { dec.decode("") }
    result.left.value.head shouldBe a[DecodingError.ParseError]
  }

  it should "raise ParseError for JSON with missing required fields" in {
    val dec    = summon[BodyDecoder[User]]
    val result = Raise.either[List[DecodingError], User] { dec.decode("""{"name":"Alice"}""") }
    result.isLeft shouldBe true
    result.left.get.head shouldBe a[DecodingError.ParseError]
  }

  it should "work with nested case classes" in {
    val dec    = summon[BodyDecoder[Person]]
    val json   = """{"name":"Alice","address":{"street":"123 Main St","city":"Springfield"}}"""
    val result = Raise.either { dec.decode(json) }
    result shouldBe Right(Person("Alice", Address("123 Main St", "Springfield")))
  }

  it should "resolve without a BodyEncoder import" in {
    assertCompiles("""
      import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
      import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
      import in.rcard.yaes.http.core.BodyDecoder
      import in.rcard.yaes.http.jsoniter.given

      case class DecodeOnly(value: String)
      given JsonValueCodec[DecodeOnly] = JsonCodecMaker.make

      summon[BodyDecoder[DecodeOnly]]
    """)
  }
}
