package in.rcard.yaes.test.scalatest

import in.rcard.yaes.{Log, Logger}

/** Mixin trait providing a no-op [[Log]] given instance for ScalaTest specs.
  *
  * Mix this trait into a ScalaTest spec class to suppress all logging output during tests. The
  * provided [[Log]] given silently discards every log message at every severity level, so tests
  * remain noise-free without requiring a real logging back-end.
  *
  * Example:
  * {{{
  * class MySpec extends AnyFlatSpec with Matchers with LogSpec {
  *
  *   "myFunction" should "run without logging errors" in {
  *     val logger = Log.getLogger("test")
  *     logger.info("this message is silently discarded")
  *   }
  * }
  * }}}
  */
trait LogSpec {

  /** A no-op [[Log]] given instance that silently discards all log messages.
    *
    * Provided automatically when this trait is mixed in, it satisfies any `using Log` context
    * parameter in the code under test without emitting any output.
    */
  given Log = new Log.Unsafe {
    override def getLogger(loggerName: String): Logger = new Logger {
      override val name: String                              = loggerName
      override def trace(msg: => String)(using Log): Unit   = ()
      override def debug(msg: => String)(using Log): Unit   = ()
      override def info(msg: => String)(using Log): Unit    = ()
      override def warn(msg: => String)(using Log): Unit    = ()
      override def error(msg: => String)(using Log): Unit   = ()
      override def fatal(msg: => String)(using Log): Unit   = ()
    }
  }
}
