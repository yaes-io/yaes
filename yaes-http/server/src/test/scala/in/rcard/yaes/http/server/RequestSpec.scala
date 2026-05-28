package in.rcard.yaes.http.server

import in.rcard.yaes.http.core.Method
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RequestSpec extends AnyFlatSpec with Matchers {

  private val baseRequest = Request(
    method = Method.GET,
    path = "/",
    headers = Map("x-api-key" -> "secret", "x-ibm-client-id" -> "abc123"),
    body = "",
    queryString = Map.empty
  )

  "Request.header" should "look up headers case-insensitively" in {
    baseRequest.header("X-Api-Key") shouldBe Some("secret")
    baseRequest.header("x-api-key") shouldBe Some("secret")
    baseRequest.header("X-API-KEY") shouldBe Some("secret")
  }

  it should "look up headers case-insensitively under Turkish locale" in {
    val defaultLocale = java.util.Locale.getDefault
    try {
      java.util.Locale.setDefault(new java.util.Locale("tr", "TR"))
      baseRequest.header("X-API-Key") shouldBe Some("secret")
      baseRequest.header("X-IBM-Client-Id") shouldBe Some("abc123")
    } finally {
      java.util.Locale.setDefault(defaultLocale)
    }
  }
}
