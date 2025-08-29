package org.aulune
package permissions.domain

/** Name of permission. */
opaque type PermissionName <: String = String


object PermissionName:
  private val nameRegex = "^[A-Za-z_-]+$".r

  /** Returns [[PermissionName]] if argument is valid, i.e.:
   *    - Name is non-empty
   *    - Consists only of latin letters and `_` and `-` symbols.
   *  @param name permission name.
   */
  def apply(name: String): Option[PermissionName] =
    Option.when(nameRegex.matches(name))(name)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param name permission name.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(name: String): PermissionName = PermissionName(name) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
