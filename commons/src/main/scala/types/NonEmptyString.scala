package org.aulune.commons
package types


/** Non-empty string. */
opaque type NonEmptyString <: String = String


object NonEmptyString:
  /** Validates string to be non-empty.
   *  @param string string.
   *  @return [[NonEmptyString]] if string is non-empty.
   */
  def apply(string: String): Option[NonEmptyString] =
    Option.when(string.nonEmpty)(string)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param string string.
   *  @throws IllegalArgumentException when empty string was given.
   */
  def unsafe(string: String): NonEmptyString = NonEmptyString(string) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
