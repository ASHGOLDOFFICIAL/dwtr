package org.aulune
package auth.domain.model


import auth.domain.errors.UserValidationError
import auth.domain.model.Username

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*


/** User representation.
 *  @param username unique username.
 *  @param hashedPassword password hash.
 *  @param groups set of groups this user belongs to.
 */
final case class User private (
    username: Username,
    hashedPassword: String,
    groups: Set[Group],
)


object User:
  private type ValidationResult[A] = ValidatedNec[UserValidationError, A]

  /** Returns a user if all given arguments are valid.
   *  @param username unique username.
   *  @param hashedPassword password hash.
   *  @param groups user groups.
   *  @return user validation result.
   */
  def apply(
      username: String,
      hashedPassword: String,
      groups: Set[Group],
  ): ValidationResult[User] = (
    Username(username).toValidNec(UserValidationError.InvalidUsername),
    hashedPassword.validNec,
    groups.validNec,
  ).mapN(new User(_, _, _))
