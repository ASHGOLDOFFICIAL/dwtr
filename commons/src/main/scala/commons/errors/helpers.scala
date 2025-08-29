package org.aulune
package commons.errors


import commons.repositories.RepositoryError

import org.aulune
import sttp.model.StatusCode


/** Converts [[RepositoryError]] to corresponding [[ApplicationServiceError]].
 *
 *  @param err repository error.
 *  @return corresponding application error.
 */
def toApplicationError(err: Throwable): ApplicationServiceError = err match
  case e: ApplicationServiceError    => e
  case RepositoryError.AlreadyExists => ApplicationServiceError.AlreadyExists
  case RepositoryError.FailedPrecondition =>
    ApplicationServiceError.FailedPrecondition
  case RepositoryError.Internal => ApplicationServiceError.Internal


/** Converts [[ApplicationServiceError]] to corresponding [[StatusCode]].
 *  @param err application error.
 *  @return corresponding status code.
 */
def toErrorResponse(
    err: ApplicationServiceError,
): StatusCode = err match
  // Should br 499 by Google's AIP, but it's not a standard code.
  case ApplicationServiceError.Cancelled       => StatusCode.RequestTimeout
  case ApplicationServiceError.Unknown         => StatusCode.InternalServerError
  case ApplicationServiceError.InvalidArgument => StatusCode.BadRequest
  case ApplicationServiceError.DeadlineExceeded   => StatusCode.GatewayTimeout
  case ApplicationServiceError.NotFound           => StatusCode.NotFound
  case ApplicationServiceError.AlreadyExists      => StatusCode.Conflict
  case ApplicationServiceError.PermissionDenied   => StatusCode.Forbidden
  case ApplicationServiceError.Unauthenticated    => StatusCode.Unauthorized
  case ApplicationServiceError.ResourceExhausted  => StatusCode.TooManyRequests
  case ApplicationServiceError.FailedPrecondition => StatusCode.BadRequest
  case ApplicationServiceError.Aborted            => StatusCode.Conflict
  case ApplicationServiceError.OutOfRange         => StatusCode.BadRequest
  case ApplicationServiceError.Unimplemented      => StatusCode.NotImplemented
  case ApplicationServiceError.Internal    => StatusCode.InternalServerError
  case ApplicationServiceError.Unavailable => StatusCode.ServiceUnavailable
  case ApplicationServiceError.DataLoss    => StatusCode.InternalServerError
