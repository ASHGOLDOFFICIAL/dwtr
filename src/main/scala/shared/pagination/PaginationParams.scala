package org.aulune
package shared.pagination

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*


final case class PaginationParams[A] private (
    pageSize: Int,
    pageToken: Option[CursorToken[A]]
)


object PaginationParams:
  private type ValidationResult[A] = ValidatedNec[PaginationValidationError, A]

  private def validatePageSize(maxSize: Int, size: Int) = Validated.condNec(
    size > 0 && size <= maxSize,
    size,
    PaginationValidationError.InvalidPageSize)

  private def validatePageToken[A: TokenDecoder](
      maybeToken: Option[String]
  ): ValidationResult[Option[CursorToken[A]]] = maybeToken.traverse { str =>
    CursorToken
      .decode(str)
      .toValidNec(PaginationValidationError.InvalidPageToken)
  }

  def apply[A: TokenDecoder](maxPageSize: Int)(
      pageSize: Int,
      pageToken: Option[String]
  ): ValidationResult[PaginationParams[A]] = (
    validatePageSize(maxPageSize, pageSize),
    validatePageToken(pageToken)
  ).mapN(PaginationParams.apply)
