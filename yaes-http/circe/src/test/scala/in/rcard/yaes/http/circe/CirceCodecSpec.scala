package in.rcard.yaes.http.circe

import in.rcard.yaes.*
import in.rcard.yaes.http.core.{BodyCodec, DecodingError}
import io.circe.{Encoder, Decoder}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CirceCodecSpec extends AnyFlatSpec with Matchers {

  case class User(name: String, age: Int) derives Encoder.AsObject, Decoder

  case class Address(street: String, city: String) derives Encoder.AsObject, Decoder
  case class Person(name: String, address: Address) derives Encoder.AsObject, Decoder

  "circeBodyCodec" should "encode a case class to compact JSON" in {
    val codec = summon[BodyCodec[User]]
    val user = User("Alice", 30)
    codec.encode(user) shouldBe """{"name":"Alice","age":30}"""
  }

  it should "decode valid JSON to a case class" in {
    val codec = summon[BodyCodec[User]]
    val result = Raise.either {
      codec.decode("""{"name":"Bob","age":25}""")
    }
    result shouldBe Right(User("Bob", 25))
  }

  it should "raise ParseError for malformed JSON" in {
    val codec = summon[BodyCodec[User]]
    val result = Raise.either[List[DecodingError], User] {
      codec.decode("not json at all")
    }
    result.isLeft shouldBe true
    result.left.get.head shouldBe a[DecodingError.ParseError]
    result.left.get.head.asInstanceOf[DecodingError.ParseError].cause shouldBe defined
  }

  it should "raise ValidationError for JSON with missing fields" in {
    val codec = summon[BodyCodec[User]]
    val result = Raise.either[List[DecodingError], User] {
      codec.decode("""{"name":"Alice"}""")
    }
    result.isLeft shouldBe true
    result.left.get.head shouldBe a[DecodingError.ValidationError]
  }

  it should "accumulate multiple decoding errors" in {
    case class Person(name: String, age: Int)
    given io.circe.Decoder[Person] = io.circe.Decoder.forProduct2("name", "age")(Person.apply)
    given io.circe.Encoder[Person] = io.circe.Encoder.forProduct2("name", "age")(p => (p.name, p.age))
    val codec = summon[BodyCodec[Person]]

    val result = Raise.either[List[DecodingError], Person] {
      codec.decode("""{"age":"not-an-int"}""")
    }

    result.isLeft shouldBe true
    val errors = result.left.toOption.get
    errors.size should be >= 2
    errors.collect { case DecodingError.ValidationError(msg) => msg }.size shouldBe errors.size
  }

  it should "have content type application/json" in {
    val codec = summon[BodyCodec[User]]
    codec.contentType shouldBe "application/json"
  }

  it should "work with semi-automatic derivation" in {
    case class Product(id: Long, label: String)
    given Encoder[Product] = Encoder.AsObject.derived
    given Decoder[Product] = Decoder.derived

    val codec = summon[BodyCodec[Product]]
    val product = Product(42L, "Widget")

    codec.encode(product) shouldBe """{"id":42,"label":"Widget"}"""

    val result = Raise.either {
      codec.decode("""{"id":42,"label":"Widget"}""")
    }
    result shouldBe Right(product)
  }

  it should "work with nested case classes" in {
    val codec = summon[BodyCodec[Person]]
    val person = Person("Alice", Address("123 Main St", "Springfield"))

    val json = codec.encode(person)
    json shouldBe """{"name":"Alice","address":{"street":"123 Main St","city":"Springfield"}}"""

    val result = Raise.either {
      codec.decode(json)
    }
    result shouldBe Right(person)
  }
}
