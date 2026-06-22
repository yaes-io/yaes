package io.yaes

/** A non-empty list type that guarantees at least one element.
  *
  * Represented as an opaque type over a tuple of head and tail to prevent external access to
  * the underlying representation.
  *
  * @example
  * {{{
  * val nel = NonEmptyList.of(1, 2, 3)
  * nel.head // 1
  * nel.tail // List(2, 3)
  * nel.toList // List(1, 2, 3)
  * }}}
  */
opaque type NonEmptyList[A] = (A, List[A])

object NonEmptyList {

  /** Creates a NonEmptyList from a head element and zero or more tail elements.
    *
    * @param head
    *   the first (mandatory) element
    * @param tail
    *   additional elements (varargs)
    * @return
    *   a NonEmptyList containing head followed by tail elements
    */
  def of[A](head: A, tail: A*): NonEmptyList[A] = (head, tail.toList)

  /** Creates a NonEmptyList with a single element.
    *
    * @param value
    *   the sole element
    * @return
    *   a NonEmptyList containing only value
    */
  def one[A](value: A): NonEmptyList[A] = (value, Nil)

  /** Safely lifts a List into a NonEmptyList.
    *
    * @param list
    *   the list to lift
    * @return
    *   Some(NonEmptyList) if list is non-empty, None otherwise
    */
  def fromList[A](list: List[A]): Option[NonEmptyList[A]] = list match
    case Nil          => None
    case head :: tail => Some((head, tail))

  extension [A](nel: NonEmptyList[A]) {

    /** Returns the first element of the list. */
    def head: A = nel._1

    /** Returns all elements except the first. */
    def tail: List[A] = nel._2

    /** Converts this NonEmptyList to a standard List. */
    def toList: List[A] = nel._1 :: nel._2

    /** Transforms each element using the given function.
      *
      * @param f
      *   the transformation function
      * @return
      *   a new NonEmptyList with f applied to every element
      */
    def map[B](f: A => B): NonEmptyList[B] = (f(nel._1), nel._2.map(f))

    /** Applies f to each element and concatenates the results.
      *
      * The result is always non-empty because f applied to the head is guaranteed non-empty.
      *
      * @param f
      *   function from element to NonEmptyList
      * @return
      *   the concatenated NonEmptyList
      */
    def flatMap[B](f: A => NonEmptyList[B]): NonEmptyList[B] = {
      val headResult: NonEmptyList[B] = f(nel._1)
      val tailResults: List[B]        = nel._2.flatMap { a => val r = f(a); r._1 :: r._2 }
      (headResult._1, headResult._2 ++ tailResults)
    }
  }
}
