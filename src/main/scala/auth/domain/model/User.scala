package org.aulune
package auth.domain.model


import auth.domain.errors.UserValidationError

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*


final case class User private[model] (
    username: String,
    hashedPassword: String,
    role: Role,
)


object User:
  private type ValidationResult[A] = ValidatedNec[UserValidationError, A]

  private val usernameRegex = "^[A-Za-z0-9_-]{8,30}$".r

  private def validateUsername(username: String): ValidationResult[String] =
    Validated.condNec(
      usernameRegex.matches(username),
      username,
      UserValidationError.InvalidUsername)

  def apply(
      username: String,
      hashedPassword: String,
      role: Role,
  ): ValidationResult[User] = (
    validateUsername(username),
    hashedPassword.validNec,
    role.validNec,
  ).mapN(new User(_, _, _))

  def unsafeApply(
      username: String,
      hashedPassword: String,
      role: Role,
  ): User = new User(username, hashedPassword, role)
