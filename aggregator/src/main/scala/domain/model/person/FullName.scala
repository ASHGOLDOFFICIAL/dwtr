package org.aulune.aggregator
package domain.model.person

/** Full name for a person. */
opaque type FullName <: String = String


object FullName:
  /** Returns [[FullName]] if argument is valid. Only allows non-empty strings.
   *  @param value title.
   */
  def apply(name: String): Option[FullName] = Option.when(name.nonEmpty)(name)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param name person's name.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(name: String): FullName = apply(name) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
