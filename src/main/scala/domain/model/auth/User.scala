package org.aulune
package domain.model.auth


import cats.data.ValidatedNec
import cats.syntax.all.*

import java.util.UUID
import scala.util.{Failure, Success, Try}


case class UserId(value: UUID) extends AnyVal

case class User private[auth] (id: UserId, role: Role)


object User:
  private type UserValidationResult[A] = ValidatedNec[UserValidationError, A]

  private def validateId(id: String): UserValidationResult[UserId] =
    Try(UUID.fromString(id)) match
      case Failure(_)    => UserValidationError.InvalidId.invalidNec
      case Success(uuid) => UserId(uuid).validNec

  def apply(id: String, role: Role): UserValidationResult[User] = (
    validateId(id),
    role.validNec[UserValidationError],
  ).mapN(User.apply)
