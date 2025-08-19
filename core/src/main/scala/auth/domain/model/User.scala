package org.aulune
package auth.domain.model


import auth.domain.errors.UserValidationError
import auth.domain.model.Username

import cats.data.ValidatedNec
import cats.syntax.all.*


/** User representation.
 *  @param username unique username.
 *  @param hashedPassword password hash (if user has it).
 *  @param groups set of groups this user belongs to.
 *  @param googleId ID in Google's services (if user linked accounts).
 */
final case class User private (
    username: Username,
    hashedPassword: Option[String],
    groups: Set[Group],
    googleId: Option[String],
)


object User:
  private type ValidationResult[A] = ValidatedNec[UserValidationError, A]

  /** Returns a user if all given arguments are valid.
   *  @param username unique username.
   *  @return user validation result.
   */
  def apply(username: String): ValidationResult[User] = Username(username)
    .toValidNec(UserValidationError.InvalidUsername)
    .map(
      new User(_, None, Set.empty, None),
    )

  /** Adds Google account to user with validation.
   *  @param user user.
   *  @param id ID in Google's services.
   *  @return validation result,
   */
  def linkGoogleId(user: User, id: String): ValidationResult[User] =
    user.copy(googleId = Some(id)).validNec

  /** Unsafe constructor for always valid boundary. */
  def unsafe(
      username: Username,
      hashedPassword: Option[String],
      groups: Set[Group],
      googleId: Option[String],
  ): User = User(
    username = username,
    hashedPassword = hashedPassword,
    groups = groups,
    googleId = googleId)
