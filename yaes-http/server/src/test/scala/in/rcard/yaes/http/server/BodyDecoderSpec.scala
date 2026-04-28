package in.rcard.yaes.http.server

import in.rcard.yaes.*
import in.rcard.yaes.http.core.{BodyDecoder, DecodingError}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BodyDecoderSpec extends AnyFlatSpec with Matchers {

  "BodyDecoder[String]" should "decode strings" in {
    val decoder = summon[BodyDecoder[String]]
    val result = Raise.either {
      decoder.decode("hello")
    }
    result shouldBe Right("hello")
  }

  it should "decode empty strings" in {
    val decoder = summon[BodyDecoder[String]]
    val result = Raise.either {
      decoder.decode("")
    }
    result shouldBe Right("")
  }

  "BodyDecoder[Int]" should "decode valid integer strings" in {
    val decoder = summon[BodyDecoder[Int]]
    val result = Raise.either {
      decoder.decode("123")
    }
    result shouldBe Right(123)
  }

  it should "raise List[DecodingError] for invalid integers" in {
    val decoder = summon[BodyDecoder[Int]]
    val result = Raise.either[List[DecodingError], Int] {
      decoder.decode("not a number")
    }
    result.isLeft shouldBe true
    result.left.get shouldBe List(DecodingError.ParseError("Invalid integer: not a number"))
  }

  "BodyDecoder[Long]" should "decode valid long strings" in {
    val decoder = summon[BodyDecoder[Long]]
    val result = Raise.either {
      decoder.decode("987654321")
    }
    result shouldBe Right(987654321L)
  }

  it should "raise List[DecodingError] for invalid longs" in {
    val decoder = summon[BodyDecoder[Long]]
    val result = Raise.either[List[DecodingError], Long] {
      decoder.decode("not a long")
    }
    result.isLeft shouldBe true
    result.left.get shouldBe List(DecodingError.ParseError("Invalid long: not a long"))
  }

  "BodyDecoder[Double]" should "decode valid double strings" in {
    val decoder = summon[BodyDecoder[Double]]
    val result = Raise.either {
      decoder.decode("2.718")
    }
    result shouldBe Right(2.718)
  }

  it should "raise List[DecodingError] for invalid doubles" in {
    val decoder = summon[BodyDecoder[Double]]
    val result = Raise.either[List[DecodingError], Double] {
      decoder.decode("not a double")
    }
    result.isLeft shouldBe true
    result.left.get shouldBe List(DecodingError.ParseError("Invalid double: not a double"))
  }

  "BodyDecoder[Boolean]" should "decode valid boolean strings" in {
    val decoder = summon[BodyDecoder[Boolean]]
    val resultTrue = Raise.either {
      decoder.decode("true")
    }
    val resultFalse = Raise.either {
      decoder.decode("false")
    }
    resultTrue shouldBe Right(true)
    resultFalse shouldBe Right(false)
  }

  it should "raise List[DecodingError] for invalid booleans" in {
    val decoder = summon[BodyDecoder[Boolean]]
    val result = Raise.either[List[DecodingError], Boolean] {
      decoder.decode("not a boolean")
    }
    result.isLeft shouldBe true
    result.left.get shouldBe List(DecodingError.ParseError("Invalid boolean: not a boolean"))
  }

  // Custom decoder for testing
  case class User(name: String, age: Int)

  given BodyDecoder[User] with {
    def decode(body: String): User raises List[DecodingError] = {
      val namePattern = """"name":"([^"]+)"""".r
      val agePattern  = """"age":(\d+)""".r

      val nameOpt = namePattern.findFirstMatchIn(body).map(_.group(1))
      val ageOpt  = agePattern.findFirstMatchIn(body).flatMap(m => m.group(1).toIntOption)

      (nameOpt, ageOpt) match {
        case (Some(name), Some(age)) => User(name, age)
        case _ => Raise.raise(List(DecodingError.ParseError(s"Invalid User JSON: $body")))
      }
    }
  }

  "Custom BodyDecoder[User]" should "decode valid JSON to User" in {
    val decoder = summon[BodyDecoder[User]]
    val result = Raise.either {
      decoder.decode("""{"name":"Bob","age":25}""")
    }
    result shouldBe Right(User("Bob", 25))
  }

  it should "raise List[DecodingError] for invalid JSON" in {
    val decoder = summon[BodyDecoder[User]]
    val result = Raise.either[List[DecodingError], User] {
      decoder.decode("""{"invalid":"json"}""")
    }
    result.isLeft shouldBe true
    result.left.get shouldBe List(DecodingError.ParseError("""Invalid User JSON: {"invalid":"json"}"""))
  }

  it should "raise List[DecodingError] for malformed JSON" in {
    val decoder = summon[BodyDecoder[User]]
    val result = Raise.either[List[DecodingError], User] {
      decoder.decode("not json at all")
    }
    result.isLeft shouldBe true
    result.left.get shouldBe List(DecodingError.ParseError("Invalid User JSON: not json at all"))
  }
}
