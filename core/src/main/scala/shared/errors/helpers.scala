package org.aulune
package shared.errors


import org.aulune
import sttp.model.StatusCode


/** Converts [[RepositoryError]] to corresponding [[ApplicationServiceError]].
 *
 *  @param err repository error.
 *  @return corresponding application error.
 */
def toApplicationError(
    err: RepositoryError,
): ApplicationServiceError = err match
  case RepositoryError.AlreadyExists  => ApplicationServiceError.AlreadyExists
  case RepositoryError.NotFound       => ApplicationServiceError.NotFound
  case RepositoryError.StorageFailure => ApplicationServiceError.InternalError


/** Converts [[ApplicationServiceError]] to corresponding [[StatusCode]].
 *  @param err application error.
 *  @return corresponding status code.
 */
def toErrorResponse(
    err: ApplicationServiceError,
): StatusCode = err match
  case ApplicationServiceError.BadRequest       => StatusCode.BadRequest
  case ApplicationServiceError.AlreadyExists    => StatusCode.Conflict
  case ApplicationServiceError.NotFound         => StatusCode.NotFound
  case ApplicationServiceError.PermissionDenied => StatusCode.Forbidden
  case ApplicationServiceError.InternalError => StatusCode.InternalServerError
