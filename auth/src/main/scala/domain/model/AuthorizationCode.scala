package org.aulune.auth
package domain.model

/** Authorization code for OAuth. */
opaque type AuthorizationCode <: String = String


object AuthorizationCode:
  /** Returns [[AuthorizationCode]] if given argument is valid.
   *  @param code authorization code.
   */
  def apply(code: String): Option[AuthorizationCode] =
    Option.when(code.nonEmpty)(code)

  /** Unsafe constructor to use within always-valid boundary.
   *  @param code authorization code.
   *  @throws IllegalArgumentException if invalid arguments are given.
   */
  def unsafe(code: String): AuthorizationCode = AuthorizationCode(code) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
