package io.yaes.http.server

import io.yaes.*
import io.yaes.http.server.params.path.*
import io.yaes.http.server.params.query.NoQueryParams
import io.yaes.http.server.routing.Route
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import PathBuilder.given

class ProtoNameSpec extends AnyFlatSpec with Matchers {

  // Prototype #1: transparent inline `param` must preserve the singleton name "id"
  // in the route type. If it works, this exact annotation compiles with NO wildcard.
  "param" should "preserve the singleton name in the Route type" in {
    val route: Route[::["id", String, NoParams], NoQueryParams] =
      POST(p"/copies" / param[String]("id") / "damaged") { (req, id: String) =>
        Response.ok("Ok")
      }
    route.toString should include("/copies")
  }

  it should "reject a wrong singleton name (proves the name is really captured)" in {
    assertDoesNotCompile(
      """
      val bad: Route[::["wrong", String, NoParams], NoQueryParams] =
        POST(p"/copies" / param[String]("id") / "damaged") { (req, id: String) =>
          Response.ok("Ok")
        }
      """
    )
  }
}
