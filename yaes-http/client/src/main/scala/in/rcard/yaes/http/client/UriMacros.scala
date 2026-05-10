package in.rcard.yaes.http.client

import scala.quoted.*
import java.net.URISyntaxException

object UriMacros:

  def uriImpl(sc: Expr[StringContext], args: Expr[Seq[UriParam]])(using Quotes): Expr[Uri] =
    import quotes.reflect.*

    // The sc param arrives as Inlined(..., Ident("sc$proxy")) — a proxy lifted by the
    // inline expansion. Follow the symbol to its ValDef initializer to reach the
    // actual StringContext(...) constructor call containing the literal parts.
    def unwrap(t: Term): Term = t match
      case Inlined(_, _, inner)           => unwrap(inner)
      case id @ Ident(_) =>
        id.symbol.tree match
          case ValDef(_, _, Some(rhs))    => unwrap(rhs)
          case _                          => t
      case _                              => t

    val parts: List[String] = unwrap(sc.asTerm) match
      case Apply(_, args) =>
        args.flatMap {
          case Literal(StringConstant(s))     => List(s)
          case Typed(Repeated(elems, _), _)   =>
            elems.map {
              case Literal(StringConstant(s)) => s
              case _ => report.errorAndAbort("uri interpolator requires string literal parts")
            }
          case _ => report.errorAndAbort("uri interpolator requires string literal parts")
        }
      case t => report.errorAndAbort(s"uri interpolator requires a string literal template")

    // Validate the static template at compile time using "x" as a safe placeholder.
    // URL-encoded path segment args only produce characters valid in path positions,
    // so if the skeleton is valid the assembled URI will be valid at runtime.
    val template = parts.mkString("x")
    try new java.net.URI(template)
    catch
      case e: URISyntaxException =>
        report.errorAndAbort(s"Invalid URI template: ${e.getMessage}")

    '{ Uri.fromTrustedString($sc.s($args.map(_.encoded)*)) }
