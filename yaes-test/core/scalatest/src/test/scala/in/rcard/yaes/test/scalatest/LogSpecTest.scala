package in.rcard.yaes.test.scalatest

import in.rcard.yaes.Log
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LogSpecTest extends AnyFlatSpec with Matchers with LogSpec {

  "LogSpec" should "provide a Log given instance" in {
    summon[Log] should not be null
  }

  it should "return a Logger with the requested name" in {
    val logger = Log.getLogger("myLogger")
    logger.name shouldBe "myLogger"
  }

  it should "not throw when logging at trace level" in {
    val logger = Log.getLogger("test")
    noException should be thrownBy logger.trace("trace message")
  }

  it should "not throw when logging at debug level" in {
    val logger = Log.getLogger("test")
    noException should be thrownBy logger.debug("debug message")
  }

  it should "not throw when logging at info level" in {
    val logger = Log.getLogger("test")
    noException should be thrownBy logger.info("info message")
  }

  it should "not throw when logging at warn level" in {
    val logger = Log.getLogger("test")
    noException should be thrownBy logger.warn("warn message")
  }

  it should "not throw when logging at error level" in {
    val logger = Log.getLogger("test")
    noException should be thrownBy logger.error("error message")
  }

  it should "not throw when logging at fatal level" in {
    val logger = Log.getLogger("test")
    noException should be thrownBy logger.fatal("fatal message")
  }
}
