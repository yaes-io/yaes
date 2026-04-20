package in.rcard.yaes

import org.scalatest.TryValues.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AsyncFlatSpec
import in.rcard.yaes.Random.*
import in.rcard.yaes.Sync.*

class RandomSpec extends AsyncFlatSpec with Matchers {

  "The Random effect" should "be able to generate a random integer" in {
    val randomInt: Random ?=> Int = Random {
      Random.nextInt
    }

    val actualResult = Random.run(randomInt)

    actualResult shouldBe a[Int]
  }

  "The Random effect" should "be able to generate a random boolean" in {
    val randomBool: Random ?=> Boolean = Random {
      Random.nextBoolean
    }

    val actualResult = Random.run(randomBool)

    actualResult shouldBe a[Boolean]
  }

  it should "be able to generate a random double" in {
    val randomDouble: Random ?=> Double = Random {
      Random.nextDouble
    }

    val actualResult = Random.run(randomDouble)

    actualResult shouldBe a[Double]
  }

  it should "be able to generate a random long" in {
    val randomLong: Random ?=> Long = Random {
      Random.nextLong
    }

    val actualResult = Random.run(randomLong)

    actualResult shouldBe a[Long]
  }

  it should "be able to run a block that does not generate a random number" in {
    val randomInt: Random ?=> Int = Random {
      42
    }

    val actualResult = Random.run(randomInt)

    actualResult shouldBe 42
  }

  it should "generate a UUID in canonical format" in {
    val uuid = Random.run(Random { Random.nextUuid })

    uuid should fullyMatch regex
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
  }

  it should "generate a UUID with version nibble 4" in {
    val uuid = Random.run(Random { Random.nextUuid })

    uuid.charAt(14) shouldBe '4'
  }

  it should "generate a UUID with variant bits 10xx" in {
    val uuid         = Random.run(Random { Random.nextUuid })
    val variantNibble = Integer.parseInt(uuid.charAt(19).toString, 16)

    (variantNibble & 0xc) shouldBe 0x8
  }

  it should "produce a deterministic UUID from a stub Random.Unsafe" in {
    val stub = new Random.Unsafe {
      private val longs = Iterator(
        0x0123456789abcdefL,
        0xfedcba9876543210L
      )
      override def nextInt(): Int         = 0
      override def nextBoolean(): Boolean = false
      override def nextDouble(): Double   = 0.0
      override def nextLong(): Long       = longs.next()
    }

    val uuid = Random.nextUuid(using stub)

    uuid shouldBe "01234567-89ab-4def-bedc-ba9876543210"
  }

  it should "compose with other effects" in {
    val program: (Sync, Random) ?=> Int = {
      val io = Sync { 42 }
      Random {
        if (io != 42) io
        else scala.util.Random.nextInt()
      }
    }

    for {
      actualResult <- Sync.run { Random.run { program } }
    } yield actualResult shouldBe a[Int]
  }
}
