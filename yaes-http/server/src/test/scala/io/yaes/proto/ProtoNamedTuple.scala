package io.yaes.proto

import scala.NamedTuple.{NamedTuple, AnyNamedTuple, Names, DropNames}

// Prototype #2b: named tuples (Scala 3.7+) as the path-param notation.
//
//   "userId" @@ Int :: "postId" @@ Long :: NoParams     // #2 binary encoding
//   (userId: Int, postId: Long)                         // #2b named tuple
object ProtoNamedTuple {

  // (a) multi-param notation compiles
  type Two = (userId: Int, postId: Long)

  // (b) single-param edge case compiles
  type One = (id: String)

  // (c) names are recoverable at the type level as a tuple of string singletons
  summon[Names[Two] =:= ("userId", "postId")]
  summon[Names[One] =:= Tuple1["id"]]

  // (d) value types are recoverable (drives the handler signature)
  summon[DropNames[Two] =:= (Int, Long)]
  summon[DropNames[One] =:= Tuple1[String]]

  // (e) "append a param" = concat names + values, then rebuild -> order & names kept
  type Append[A <: AnyNamedTuple, B <: AnyNamedTuple] =
    NamedTuple[Tuple.Concat[Names[A], Names[B]], Tuple.Concat[DropNames[A], DropNames[B]]]
  summon[Append[One, Two] =:= (id: String, userId: Int, postId: Long)]

  // (f) QueryParams: same shape. Optional/repeated params ride in the element type
  //     (Option[_] / List[_]), which is already how the current encoding models them.
  type Q = (q: String, limit: Option[Int], tag: List[String])
  summon[Names[Q] =:= ("q", "limit", "tag")]
  summon[DropNames[Q] =:= (String, Option[Int], List[String])]
}
