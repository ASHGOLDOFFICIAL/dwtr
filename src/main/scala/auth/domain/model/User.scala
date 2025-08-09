package org.aulune
package auth.domain.model


import auth.domain.errors.UserValidationError
import auth.domain.model.Username

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*


/** User representation.
 *  @param username unique username.
 *  @param hashedPassword password hash.
 *  @param role user role.
 */
final case class User private (
    username: Username,
    hashedPassword: String,
    role: Role,
)


object User:
  private type ValidationResult[A] = ValidatedNec[UserValidationError, A]

  /** Returns a user if all given arguments are valid.
   *  @param username unique username.
   *  @param hashedPassword password hash.
   *  @param role user role.
   *  @return user validation result.
   */
  def apply(
      username: String,
      hashedPassword: String,
      role: Role,
  ): ValidationResult[User] = (
    Username(username).toValidNec(UserValidationError.InvalidUsername),
    hashedPassword.validNec,
    role.validNec,
  ).mapN(new User(_, _, _))

  /** Creates new user without argument validation.
   *
   *  Should only be used within always-valid boundary (persistence layer for
   *  example).
   *  @param username username.
   *  @param hashedPassword password hash.
   *  @param role user's role.
   *  @return a user.
   */
  def unsafeApply(
      username: String,
      hashedPassword: String,
      role: Role,
  ): User = new User(Username.unsafeApply(username), hashedPassword, role)
