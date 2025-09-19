package org.aulune.auth
package domain.model

/** Authorization code for OAuth. */
opaque type AuthorizationCode <: String = String


object AuthorizationCode:
  /** Returns [[AuthorizationCode]] if given argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param code authorization code.
   */
  def apply(code: String): Option[AuthorizationCode] =
    val stripped = code.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use within always-valid boundary.
   *  @param code authorization code.
   *  @throws IllegalArgumentException if invalid arguments are given.
   */
  def unsafe(code: String): AuthorizationCode = AuthorizationCode(code) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
