package io.yaes.http.client

import java.util.UUID
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.compiletime.testing.typeChecks

class UriInterpolatorSpec extends AnyFlatSpec with Matchers:

  "uri interpolator" should "interpolate a String path param" in {
    val name = "alice"
    val u    = uri"https://api.example.com/users/$name"
    u.value shouldBe "https://api.example.com/users/alice"
  }

  it should "interpolate an Int path param" in {
    val id = 42
    val u  = uri"https://api.example.com/users/$id"
    u.value shouldBe "https://api.example.com/users/42"
  }

  it should "encode spaces in path params as %20 not +" in {
    val name = "john doe"
    val u    = uri"https://api.example.com/users/$name"
    u.value shouldBe "https://api.example.com/users/john%20doe"
  }

  it should "encode slashes in path params as %2F" in {
    val segment = "a/b"
    val u       = uri"https://api.example.com/files/$segment"
    u.value shouldBe "https://api.example.com/files/a%2Fb"
  }

  it should "encode Unicode characters in path params" in {
    val name = "café"
    val u    = uri"https://api.example.com/users/$name"
    u.value shouldBe "https://api.example.com/users/caf%C3%A9"
  }

  it should "encode percent signs in path params as %25" in {
    val segment = "100%"
    val u       = uri"https://api.example.com/items/$segment"
    u.value shouldBe "https://api.example.com/items/100%25"
  }

  it should "encode question marks in path params as %3F" in {
    val segment = "what?"
    val u       = uri"https://api.example.com/items/$segment"
    u.value shouldBe "https://api.example.com/items/what%3F"
  }

  it should "encode hash in path params as %23" in {
    val segment = "tag#1"
    val u       = uri"https://api.example.com/items/$segment"
    u.value shouldBe "https://api.example.com/items/tag%231"
  }

  it should "encode plus signs in path params as %2B" in {
    val segment = "a+b"
    val u       = uri"https://api.example.com/items/$segment"
    u.value shouldBe "https://api.example.com/items/a%2Bb"
  }

  it should "interpolate multiple path params in one expression" in {
    val userId  = 7
    val orderId = "ord-99"
    val u       = uri"https://api.example.com/users/$userId/orders/$orderId"
    u.value shouldBe "https://api.example.com/users/7/orders/ord-99"
  }

  it should "interpolate a Long path param" in {
    val id = 9876543210L
    val u  = uri"https://api.example.com/records/$id"
    u.value shouldBe "https://api.example.com/records/9876543210"
  }

  it should "interpolate a Double path param" in {
    val price = 3.14
    val u     = uri"https://api.example.com/products/$price"
    u.value shouldBe "https://api.example.com/products/3.14"
  }

  it should "interpolate a UUID path param" in {
    val id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    val u  = uri"https://api.example.com/entities/$id"
    u.value shouldBe "https://api.example.com/entities/550e8400-e29b-41d4-a716-446655440000"
  }

  it should "interpolate a Boolean path param" in {
    val flag = true
    val u    = uri"https://api.example.com/features/$flag"
    u.value shouldBe "https://api.example.com/features/true"
  }

  it should "produce an empty path segment for an empty string value" in {
    val empty = ""
    val u     = uri"https://api.example.com/prefix/$empty/suffix"
    u.value shouldBe "https://api.example.com/prefix//suffix"
  }

  it should "use a custom PathParamStringifier" in {
    case class ItemId(value: Int)
    given PathParamStringifier[ItemId] with {
      def encode(v: ItemId): String = s"item-${v.value}"
    }
    val id = ItemId(5)
    val u  = uri"https://api.example.com/items/$id"
    u.value shouldBe "https://api.example.com/items/item-5"
  }

  it should "fail to compile for an invalid URI template" in {
    typeChecks("""uri"not a
valid uri"""") shouldBe false
  }

  it should "fail to compile for a type without a PathParamStringifier" in {
    typeChecks("""
      case class Foo(x: Int)
      val f = Foo(1)
      uri"https://api.example.com/$f"
    """) shouldBe false
  }
