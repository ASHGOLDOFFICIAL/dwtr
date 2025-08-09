package org.aulune
package shared.pagination


import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*


/** Parameters to use for cursor-based pagination.
 *  @param pageSize page size.
 *  @param pageToken page token which identifies last sent element and maybe
 *    sorting parameters.
 *  @tparam A token underlying type.
 */
final case class PaginationParams[A] private (
    pageSize: Int,
    pageToken: Option[CursorToken[A]],
)


object PaginationParams:
  private type ValidationResult[A] = ValidatedNec[PaginationValidationError, A]

  private def validatePageSize(maxSize: Int, size: Int) = Validated.condNec(
    size > 0 && size <= maxSize,
    size,
    PaginationValidationError.InvalidPageSize)

  private def validatePageToken[A: TokenDecoder](
      maybeToken: Option[String],
  ): ValidationResult[Option[CursorToken[A]]] = maybeToken.traverse { str =>
    CursorToken
      .decode(str)
      .toValidNec(PaginationValidationError.InvalidPageToken)
  }

  /** Validates given arguments and returns [[PaginationParams]] if all of them
   *  are valid.
   *  @param maxPageSize maximum allowed page size.
   *  @param pageSize given page size.
   *  @param pageToken page token.
   *  @tparam A page token underlying type.
   *  @return validation result with params.
   */
  def apply[A: TokenDecoder](maxPageSize: Int)(
      pageSize: Int,
      pageToken: Option[String],
  ): ValidationResult[PaginationParams[A]] = (
    validatePageSize(maxPageSize, pageSize),
    validatePageToken(pageToken),
  ).mapN(PaginationParams.apply)
