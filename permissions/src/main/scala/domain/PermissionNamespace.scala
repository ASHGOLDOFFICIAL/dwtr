package org.aulune.permissions
package domain

/** Namespace of permission. */
opaque type PermissionNamespace <: String = String


object PermissionNamespace:
  private val namespaceRegex = "^[A-Za-z_-]+$".r

  /** Returns [[PermissionNamespace]] if argument is valid, i.e.:
   *    - Namespace is non-empty.
   *    - Consists only of latin letters and `_` and `-` symbols.
   *  @param namespace permission namespace.
   */
  def apply(namespace: String): Option[PermissionNamespace] =
    Option.when(namespaceRegex.matches(namespace))(namespace)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param namespace permission namespace.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(namespace: String): PermissionNamespace =
    PermissionNamespace(namespace) match
      case Some(value) => value
      case None        => throw new IllegalArgumentException()
