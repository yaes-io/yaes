package io.yaes.http.client

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.yaes.*
import io.yaes.http.client.Uri.InvalidUri

class UriPathOpsSpec extends AnyFlatSpec with Matchers:

  private def makeUri(raw: String): Uri =
    Raise.either[InvalidUri, Uri] { Uri(raw) } match
      case Left(e)  => fail(s"Invalid URI: ${e.reason}")
      case Right(u) => u

  "/ operator" should "append a plain string segment" in {
    val base   = makeUri("https://api.example.com/v1")
    val result = base / "users"
    result.value shouldBe "https://api.example.com/v1/users"
  }

  it should "append a numeric Long segment via implicit conversion" in {
    val base   = makeUri("https://api.example.com/users")
    val id     = 42L
    val result = base / id
    result.value shouldBe "https://api.example.com/users/42"
  }

  it should "normalise a trailing slash on the base URI" in {
    val base   = makeUri("https://api.example.com/users/")
    val result = base / "profile"
    result.value shouldBe "https://api.example.com/users/profile"
  }

  it should "support chained / calls" in {
    val base   = makeUri("https://api.example.com")
    val id     = 7L
    val result = base / "users" / id
    result.value shouldBe "https://api.example.com/users/7"
  }

  it should "URL-encode segments with special characters" in {
    val base   = makeUri("https://api.example.com/search")
    val result = base / "hello world"
    result.value shouldBe "https://api.example.com/search/hello%20world"
  }

  it should "preserve an existing query string when appending a segment" in {
    val base   = makeUri("https://api.example.com/users?page=1")
    val result = base / "profile"
    result.value shouldBe "https://api.example.com/users/profile?page=1"
  }

  it should "preserve an existing fragment when appending a segment" in {
    val base   = makeUri("https://api.example.com/docs#section")
    val result = base / "intro"
    result.value shouldBe "https://api.example.com/docs/intro#section"
  }

  it should "work with a custom PathParamStringifier" in {
    case class ProductId(value: Int)
    given PathParamStringifier[ProductId] with {
      def encode(v: ProductId): String = s"prod-${v.value}"
    }
    val base   = makeUri("https://api.example.com/catalog")
    val pid    = ProductId(99)
    val result = base / pid
    result.value shouldBe "https://api.example.com/catalog/prod-99"
  }
