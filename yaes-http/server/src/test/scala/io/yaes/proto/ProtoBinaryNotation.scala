package io.yaes.proto

// Prototype #2: a real binary-infix encoding for path params.
// Goal: make the notation the docstrings *claim* actually compile.
//
//   current (3-arg cons, NOT valid infix):  ::["id", String, NoParams]
//   proto   (binary cons + entry node):      "id" @@ String :: NoParams
//
// Entry op is `@@` (first char `@` binds tighter than colon-ops), so
//   "id" @@ String :: NoParams   groups as   ("id" @@ String) :: NoParams

sealed trait PathParams
sealed trait PathEntry

/** A single named, typed parameter: Name @@ Type */
final class @@[Name <: String & Singleton, Type] extends PathEntry

/** Binary cons: Entry :: Tail */
final case class ::[H <: PathEntry, T <: PathParams]() extends PathParams
case object PathParamsNil extends PathParams
type NoParams = PathParamsNil.type

/** Type-level append, keeps left-to-right order. */
type Append[P <: PathParams, Name <: String & Singleton, Type] <: PathParams = P match
  case NoParams        => (Name @@ Type) :: NoParams
  case (h :: tail)     => h :: Append[tail, Name, Type]

object ProtoBinaryNotation {

  // (a) the promised infix notation compiles, single and multi-param
  type One = "id" @@ String :: NoParams
  type Two = "userId" @@ Int :: "postId" @@ Long :: NoParams

  // (b) grouping is correct: One is exactly ::[@@["id",String], NoParams]
  summon[One =:= ::[@@["id", String], NoParams]]

  // (c) Append builds the same shape as writing it by hand
  summon[Append[NoParams, "id", String] =:= One]
  summon[Append[Append[NoParams, "userId", Int], "postId", Long] =:= Two]
}
