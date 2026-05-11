package in.rcard.yaes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NonEmptyListSpec extends AnyFlatSpec with Matchers {

  "NonEmptyList.of" should "create a NonEmptyList with a head and tail" in {
    val nel = NonEmptyList.of(1, 2, 3)
    nel.head shouldBe 1
    nel.tail shouldBe List(2, 3)
  }

  it should "create a single-element NonEmptyList when only head is given" in {
    val nel = NonEmptyList.of(42)
    nel.head shouldBe 42
    nel.tail shouldBe Nil
  }

  "NonEmptyList.one" should "create a single-element NonEmptyList" in {
    val nel = NonEmptyList.one("hello")
    nel.head shouldBe "hello"
    nel.tail shouldBe Nil
  }

  "NonEmptyList.fromList" should "return None for an empty list" in {
    NonEmptyList.fromList(Nil) shouldBe None
  }

  it should "return Some(NonEmptyList) for a non-empty list" in {
    NonEmptyList.fromList(List(1, 2, 3)) shouldBe Some(NonEmptyList.of(1, 2, 3))
  }

  it should "return Some with a single-element NonEmptyList for a singleton list" in {
    NonEmptyList.fromList(List(7)) shouldBe Some(NonEmptyList.one(7))
  }

  "NonEmptyList.toList" should "convert to a standard List" in {
    NonEmptyList.of(1, 2, 3).toList shouldBe List(1, 2, 3)
  }

  it should "convert a single-element NonEmptyList to a singleton List" in {
    NonEmptyList.one(5).toList shouldBe List(5)
  }

  "NonEmptyList.map" should "transform all elements" in {
    val nel = NonEmptyList.of(1, 2, 3)
    nel.map(_ * 2).toList shouldBe List(2, 4, 6)
  }

  it should "transform a single-element NonEmptyList" in {
    NonEmptyList.one(3).map(_ + 1).head shouldBe 4
  }

  "NonEmptyList.flatMap" should "apply f and concatenate results" in {
    val nel = NonEmptyList.of(1, 2, 3)
    val result = nel.flatMap(x => NonEmptyList.of(x, x * 10))
    result.toList shouldBe List(1, 10, 2, 20, 3, 30)
  }

  it should "work with a single-element NonEmptyList" in {
    val nel = NonEmptyList.one(5)
    val result = nel.flatMap(x => NonEmptyList.of(x, x + 1))
    result.toList shouldBe List(5, 6)
  }

  it should "always produce a non-empty result" in {
    val nel = NonEmptyList.of(1, 2)
    val result = nel.flatMap(x => NonEmptyList.one(x * 3))
    result.toList shouldBe List(3, 6)
  }

  "NonEmptyList opaque type" should "hide the tuple representation" in {
    """
    |val nel = NonEmptyList.of(1, 2)
    |val _ = nel._1  // should not compile
    """.stripMargin shouldNot typeCheck
  }

}
