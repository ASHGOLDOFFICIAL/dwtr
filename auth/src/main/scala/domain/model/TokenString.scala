package org.aulune.auth
package domain.model

/** Non-empty token string. */
opaque type TokenString <: String = String


object TokenString:
  /** Returns [[TokenString]] if given argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param token authentication token string.
   */
  def apply(token: String): Option[TokenString] =
    val stripped = token.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use within always-valid boundary.
   *  @param token authentication token string.
   *  @throws IllegalArgumentException if invalid arguments are given.
   */
  def unsafe(token: String): TokenString = TokenString(token) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
