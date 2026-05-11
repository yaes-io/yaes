package in.rcard.yaes.http.core

import in.rcard.yaes.NonEmptyList

/** Represents errors that occur during body decoding.
  *
  * DecodingError is a sealed trait representing errors as values (not exceptions),
  * designed to work with the YAES `Raise[E]` effect for typed error handling.
  */
sealed trait DecodingError {
  def message: String
}

object DecodingError {
  /** Error parsing the body format (e.g., invalid JSON syntax).
    *
    * @param message
    *   Description of the parsing error
    * @param cause
    *   Optional underlying exception that caused the error
    */
  case class ParseError(message: String, cause: Option[Throwable] = None)
      extends DecodingError

  /** One or more validation errors collected during decoding (e.g., missing or
    * mistyped fields).
    *
    * @param reasons
    *   A non-empty list of human-readable validation failure messages
    */
  case class ValidationErrors(reasons: NonEmptyList[String]) extends DecodingError {
    def message: String = reasons.toList.mkString("; ")
  }
}
