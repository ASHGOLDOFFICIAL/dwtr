package org.aulune.auth
package domain.model


import domain.errors.UserValidationError
import domain.model.Username

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*
import org.aulune.commons.types.Uuid


/** User representation.
 *  @param id user's unique UUID.
 *  @param username unique username.
 *  @param hashedPassword password hash (if user has it).
 *  @param googleId ID in Google's services (if user linked accounts).
 */
final case class User private (
    id: Uuid[User],
    username: Username,
    hashedPassword: Option[String],
    googleId: Option[String],
)


object User:
  private type ValidationResult[A] = ValidatedNec[UserValidationError, A]

  /** Returns a user if all given arguments are valid.
   *  @param id UUID assigned to user.
   *  @param username unique username.
   *  @return user validation result.
   */
  def apply(
      id: Uuid[User],
      username: Username,
      hashedPassword: Option[String],
      googleId: Option[String],
  ): ValidationResult[User] = validateState(
    new User(
      id = id,
      username = username,
      hashedPassword = hashedPassword,
      googleId = googleId))

  /** Adds Google account to user with validation.
   *  @param user user.
   *  @param id ID in Google's services.
   *  @return validation result,
   */
  def linkGoogleId(user: User, id: String): ValidationResult[User] =
    user.copy(googleId = Some(id)).validNec

  /** Unsafe constructor for always valid boundary. */
  def unsafe(
      id: Uuid[User],
      username: Username,
      hashedPassword: Option[String],
      googleId: Option[String],
  ): User = User(
    id = id,
    username = username,
    hashedPassword = hashedPassword,
    googleId = googleId) match
    case Validated.Valid(a)   => a
    case Validated.Invalid(e) => throw e.head

  def validateState(user: User): ValidationResult[User] = user.validNec
