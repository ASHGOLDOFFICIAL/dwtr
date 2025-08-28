package org.aulune
package shared.errors


import shared.repositories.RepositoryError

import org.aulune
import sttp.model.StatusCode


/** Converts [[RepositoryError]] to corresponding [[ApplicationServiceError]].
 *
 *  @param err repository error.
 *  @return corresponding application error.
 */
def toApplicationError(err: Throwable): ApplicationServiceError = err match
  case RepositoryError.AlreadyExists   => ApplicationServiceError.AlreadyExists
  case RepositoryError.NothingToUpdate => ApplicationServiceError.NotFound
  case RepositoryError.FailedPrecondition =>
    ApplicationServiceError.FailedPrecondition
  case e: ApplicationServiceError => e


/** Converts [[ApplicationServiceError]] to corresponding [[StatusCode]].
 *  @param err application error.
 *  @return corresponding status code.
 */
def toErrorResponse(
    err: ApplicationServiceError,
): StatusCode = err match
  case ApplicationServiceError.BadRequest         => StatusCode.BadRequest
  case ApplicationServiceError.FailedPrecondition => StatusCode.BadRequest
  case ApplicationServiceError.AlreadyExists      => StatusCode.Conflict
  case ApplicationServiceError.NotFound           => StatusCode.NotFound
  case ApplicationServiceError.PermissionDenied   => StatusCode.Forbidden
