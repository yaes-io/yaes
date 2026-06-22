package io.yaes.http.server

import io.yaes.http.core.BodyEncoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BodyEncoderSpec extends AnyFlatSpec with Matchers {

  "BodyEncoder[String]" should "encode strings as-is" in {
    val encoder = summon[BodyEncoder[String]]
    encoder.encode("hello") shouldBe "hello"
  }

  it should "report text/plain content type" in {
    val encoder = summon[BodyEncoder[String]]
    encoder.contentType shouldBe "text/plain; charset=UTF-8"
  }

  "BodyEncoder[Int]" should "encode integers to their string representation" in {
    val encoder = summon[BodyEncoder[Int]]
    encoder.encode(42) shouldBe "42"
  }

  it should "report text/plain content type" in {
    val encoder = summon[BodyEncoder[Int]]
    encoder.contentType shouldBe "text/plain; charset=UTF-8"
  }

  "BodyEncoder[Long]" should "encode longs to their string representation" in {
    val encoder = summon[BodyEncoder[Long]]
    encoder.encode(123456789L) shouldBe "123456789"
  }

  it should "report text/plain content type" in {
    val encoder = summon[BodyEncoder[Long]]
    encoder.contentType shouldBe "text/plain; charset=UTF-8"
  }

  "BodyEncoder[Double]" should "encode doubles to their string representation" in {
    val encoder = summon[BodyEncoder[Double]]
    encoder.encode(3.14) shouldBe "3.14"
  }

  it should "report text/plain content type" in {
    val encoder = summon[BodyEncoder[Double]]
    encoder.contentType shouldBe "text/plain; charset=UTF-8"
  }

  "BodyEncoder[Boolean]" should "encode true to \"true\"" in {
    val encoder = summon[BodyEncoder[Boolean]]
    encoder.encode(true) shouldBe "true"
  }

  it should "encode false to \"false\"" in {
    val encoder = summon[BodyEncoder[Boolean]]
    encoder.encode(false) shouldBe "false"
  }

  it should "report text/plain content type" in {
    val encoder = summon[BodyEncoder[Boolean]]
    encoder.contentType shouldBe "text/plain; charset=UTF-8"
  }

  case class User(name: String, age: Int)

  given BodyEncoder[User] with {
    def contentType: String = "application/json"
    def encode(user: User): String =
      s"""{"name":"${user.name}","age":${user.age}}"""
  }

  "Custom BodyEncoder[User]" should "encode a User to JSON" in {
    val encoder = summon[BodyEncoder[User]]
    encoder.encode(User("Alice", 30)) shouldBe """{"name":"Alice","age":30}"""
  }

  it should "report application/json content type" in {
    val encoder = summon[BodyEncoder[User]]
    encoder.contentType shouldBe "application/json"
  }
}
