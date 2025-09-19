package org.aulune.permissions
package domain

/** Namespace of permission. */
opaque type PermissionDescription <: String = String


object PermissionDescription:
  /** Returns [[PermissionDescription]] if argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param description permission description.
   */
  def apply(description: String): Option[PermissionDescription] =
    val stripped = description.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param description permission description.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(description: String): PermissionDescription =
    PermissionDescription(description) match
      case Some(value) => value
      case None        => throw new IllegalArgumentException()
