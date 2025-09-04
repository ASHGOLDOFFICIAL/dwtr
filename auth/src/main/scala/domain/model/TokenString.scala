package org.aulune.auth
package domain.model

/** Non-empty token string. */
opaque type TokenString <: String = String


object TokenString:
  /** Returns [[TokenString]] if given argument is valid.
   *  @param token authentication token string.
   */
  def apply(token: String): Option[TokenString] =
    Option.when(token.nonEmpty)(token)

  /** Unsafe constructor to use within always-valid boundary.
   *  @param token authentication token string.
   *  @throws IllegalArgumentException if invalid arguments are given.
   */
  def unsafe(token: String): TokenString = TokenString(token) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
