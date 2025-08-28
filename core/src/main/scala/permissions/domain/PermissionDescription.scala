package org.aulune
package permissions.domain

/** Namespace of permission. */
opaque type PermissionDescription <: String = String


object PermissionDescription:
  /** Returns [[PermissionDescription]] if argument is valid, i.e. non-empty.
   *  @param description permission description.
   */
  def apply(description: String): Option[PermissionDescription] =
    Option.when(description.nonEmpty)(description)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param description permission description.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(description: String): PermissionDescription =
    PermissionDescription(description) match
      case Some(value) => value
      case None        => throw new IllegalArgumentException()
