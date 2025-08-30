package org.aulune.auth
package domain.model

/** String of length 8-30 characters with latin letters, numbers and dash. */
opaque type Username <: String = String


object Username:
  private val usernameRegex = "^[A-Za-z0-9_-]{8,30}$".r

  /** Returns a username if given string is a valid username.
   *  @param username username as string.
   */
  def apply(username: String): Option[Username] =
    Option.when(usernameRegex.matches(username))(username)

  /** Creates new username without argument validation.
   *
   *  Should only be used within always-valid boundary (persistence layer for
   *  example).
   *  @param username username.
   *  @return a username.
   */
  def unsafeApply(username: String): Username = username
