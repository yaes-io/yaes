package in.rcard.yaes.http.client

import in.rcard.yaes.*
import scala.quoted.*
import java.net.{URI, URISyntaxException, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8

/** Validated URI wrapper backed by [[java.net.URI]].
  *
  * An opaque type ensuring URIs are syntactically valid at construction time. Invalid input raises
  * [[Uri.InvalidUri]] via the [[in.rcard.yaes.Raise]] effect.
  *
  * Example:
  * {{{
  * val uri: Uri raises Uri.InvalidUri = Uri("https://example.com/api")
  * }}}
  */
opaque type Uri = java.net.URI

object Uri:

  /** Raised when a raw string cannot be parsed as a valid URI.
    *
    * @param input
    *   the invalid input string
    * @param reason
    *   the parse error message
    */
  case class InvalidUri(input: String, reason: String)

  /** Parses a raw string into a [[Uri]], raising [[InvalidUri]] on failure.
    *
    * @param raw
    *   the URI string to parse
    * @return
    *   the validated URI
    */
  def apply(raw: String): Uri raises InvalidUri =
    try new java.net.URI(raw)
    catch
      case e: URISyntaxException =>
        Raise.raise(InvalidUri(raw, e.getMessage))

  /** Constructs a [[Uri]] from a pre-validated string without raising. For macro use only. */
  private[client] def fromTrustedString(raw: String): Uri = new java.net.URI(raw)

  extension (uri: Uri)
    /** Returns the underlying [[java.net.URI]]. */
    def toJavaURI: java.net.URI = uri

    /** Returns the URI as a string. */
    def value: String = uri.toString

    /** Returns the host component, if present. */
    def host: Option[String] = Option(uri.getHost)

    /** Returns the port, defaulting to 443 for `https` and 80 for other schemes if not specified. */
    def port: Int =
      if uri.getPort != -1 then uri.getPort
      else if Option(uri.getScheme).exists(_.equalsIgnoreCase("https")) then 443
      else 80

    /** Appends a URL-encoded path segment to this URI.
      *
      * Trailing slashes on the base URI path are normalised before appending, so
      * chained calls always produce a clean path. Existing query strings and
      * fragments are preserved.
      *
      * Example:
      * {{{
      * val base = uri"https://api.example.com/users"
      * val id   = 42L
      * val u    = base / id   // https://api.example.com/users/42
      * }}}
      *
      * @param segment the path segment to append; any value with a [[PathParamStringifier]] is accepted via implicit conversion
      * @return a new [[Uri]] with the segment appended
      */
    def /(segment: UriParam): Uri =
      val raw     = uri.toASCIIString
      val qIdx    = raw.indexOf('?')
      val fIdx    = raw.indexOf('#')
      val pathEnd = (List(qIdx, fIdx).filter(_ >= 0) :+ raw.length).min
      val (pathPart, rest) = raw.splitAt(pathEnd)
      Uri.fromTrustedString(s"${pathPart.stripSuffix("/")}/${segment.encoded}$rest")

    /** Appends query parameters to the URI, URL-encoding keys and values.
      *
      * If the URI already contains a query string, parameters are appended with `&`.
      * Existing fragment components are preserved correctly.
      *
      * @param queryParams the parameters to append
      * @return a [[java.net.URI]] with the appended query string
      */
    def withQueryParams(queryParams: List[(String, String)]): URI =
      if queryParams.isEmpty then uri.toJavaURI
      else
        val encoded = queryParams.iterator.map { (k, v) =>
          s"${URLEncoder.encode(k, UTF_8)}=${URLEncoder.encode(v, UTF_8)}"
        }.mkString("&")
        val raw       = uri.toASCIIString
        val fragIdx   = raw.indexOf('#')
        val (base, fragment) =
          if fragIdx == -1 then (raw, "") else (raw.substring(0, fragIdx), raw.substring(fragIdx))
        val separator = if base.contains('?') then "&" else "?"
        new URI(s"$base$separator$encoded$fragment")

extension (sc: StringContext)
  /** String interpolator that constructs a [[Uri]] from a template with typed path parameters.
    *
    * Each interpolated argument must have an implicit [[PathParamStringifier]] in scope. The stringifier
    * converts the value to its raw string form, which is then URL-encoded by [[UriParam]] before
    * being spliced into the URI template.
    *
    * The URI template is validated at compile time. An invalid literal template is a compile error.
    * Interpolated arguments are URL-encoded and therefore always produce valid URI characters.
    *
    * Example:
    * {{{
    * val id: Long = 42L
    * val endpoint: Uri = uri"https://api.example.com/users/$id"
    * // produces a Uri with value "https://api.example.com/users/42"
    * }}}
    *
    * @param args the path parameter values to interpolate, URL-encoded via [[PathParamStringifier]]
    * @return the constructed and validated [[Uri]]
    */
  inline def uri(args: UriParam*): Uri = ${ UriMacros.uriImpl('sc, 'args) }
