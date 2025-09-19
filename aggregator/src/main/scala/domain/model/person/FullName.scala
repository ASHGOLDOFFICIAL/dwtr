package org.aulune.aggregator
package domain.model.person

/** Full name for a person. */
opaque type FullName <: String = String


object FullName:
  /** Returns [[FullName]] if argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param name person's name.
   */
  def apply(name: String): Option[FullName] =
    val stripped = name.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param name person's name.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(name: String): FullName = apply(name) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
