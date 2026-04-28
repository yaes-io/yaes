package in.rcard.yaes.http.circe

import in.rcard.yaes.*
import in.rcard.yaes.http.core.{BodyDecoder, BodyEncoder, DecodingError}
import io.circe.{Encoder, Decoder}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CirceCodecSpec extends AnyFlatSpec with Matchers {

  case class User(name: String, age: Int) derives Encoder.AsObject, Decoder

  case class Address(street: String, city: String) derives Encoder.AsObject, Decoder
  case class Person(name: String, address: Address) derives Encoder.AsObject, Decoder

  "circeBodyEncoder" should "encode a case class to compact JSON" in {
    val enc = summon[BodyEncoder[User]]
    val user = User("Alice", 30)
    enc.encode(user) shouldBe """{"name":"Alice","age":30}"""
  }

  it should "have content type application/json" in {
    val enc = summon[BodyEncoder[User]]
    enc.contentType shouldBe "application/json"
  }

  it should "work with semi-automatic derivation" in {
    case class Product(id: Long, label: String)
    given Encoder[Product] = Encoder.AsObject.derived

    val enc = summon[BodyEncoder[Product]]
    enc.encode(Product(42L, "Widget")) shouldBe """{"id":42,"label":"Widget"}"""
  }

  it should "work with nested case classes" in {
    val enc = summon[BodyEncoder[Person]]
    enc.encode(Person("Alice", Address("123 Main St", "Springfield"))) shouldBe
      """{"name":"Alice","address":{"street":"123 Main St","city":"Springfield"}}"""
  }

  it should "resolve without a Decoder in scope" in {
    case class EncodeOnly(value: String)
    given Encoder[EncodeOnly] = Encoder.AsObject.derived
    assertCompiles("summon[BodyEncoder[EncodeOnly]]")
    assertDoesNotCompile("summon[BodyDecoder[EncodeOnly]]")
  }

  "circeBodyDecoder" should "decode valid JSON to a case class" in {
    val dec = summon[BodyDecoder[User]]
    val result = Raise.either {
      dec.decode("""{"name":"Bob","age":25}""")
    }
    result shouldBe Right(User("Bob", 25))
  }

  it should "raise ParseError for malformed JSON" in {
    val dec = summon[BodyDecoder[User]]
    val result = Raise.either[List[DecodingError], User] {
      dec.decode("not json at all")
    }
    result.isLeft shouldBe true
    result.left.get.head shouldBe a[DecodingError.ParseError]
    result.left.get.head.asInstanceOf[DecodingError.ParseError].cause shouldBe defined
  }

  it should "raise ValidationError for JSON with missing fields" in {
    val dec = summon[BodyDecoder[User]]
    val result = Raise.either[List[DecodingError], User] {
      dec.decode("""{"name":"Alice"}""")
    }
    result.isLeft shouldBe true
    result.left.get.head shouldBe a[DecodingError.ValidationError]
  }

  it should "accumulate multiple decoding errors" in {
    case class PersonFlat(name: String, age: Int)
    given io.circe.Decoder[PersonFlat] =
      io.circe.Decoder.forProduct2("name", "age")(PersonFlat.apply)
    val dec = summon[BodyDecoder[PersonFlat]]

    val result = Raise.either[List[DecodingError], PersonFlat] {
      dec.decode("""{"age":"not-an-int"}""")
    }

    result.isLeft shouldBe true
    val errors = result.left.toOption.get
    errors.size should be >= 2
    errors.collect { case DecodingError.ValidationError(msg) => msg }.size shouldBe errors.size
  }

  it should "work with semi-automatic derivation" in {
    case class Product(id: Long, label: String)
    given Decoder[Product] = Decoder.derived

    val dec = summon[BodyDecoder[Product]]
    val result = Raise.either {
      dec.decode("""{"id":42,"label":"Widget"}""")
    }
    result shouldBe Right(Product(42L, "Widget"))
  }

  it should "work with nested case classes" in {
    val dec   = summon[BodyDecoder[Person]]
    val json  = """{"name":"Alice","address":{"street":"123 Main St","city":"Springfield"}}"""
    val result = Raise.either {
      dec.decode(json)
    }
    result shouldBe Right(Person("Alice", Address("123 Main St", "Springfield")))
  }

  it should "resolve without an Encoder in scope" in {
    case class DecodeOnly(value: String)
    given Decoder[DecodeOnly] = Decoder.derived
    assertCompiles("summon[BodyDecoder[DecodeOnly]]")
    assertDoesNotCompile("summon[BodyEncoder[DecodeOnly]]")
  }
}
