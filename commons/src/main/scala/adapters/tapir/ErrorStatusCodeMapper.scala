package org.aulune.commons
package adapters.tapir


import errors.ErrorStatus.{
  Aborted,
  AlreadyExists,
  Cancelled,
  DataLoss,
  DeadlineExceeded,
  FailedPrecondition,
  Internal,
  InvalidArgument,
  NotFound,
  OutOfRange,
  PermissionDenied,
  ResourceExhausted,
  Unauthenticated,
  Unavailable,
  Unimplemented,
  Unknown,
}
import errors.{ErrorResponse, ErrorStatus}

import sttp.model.StatusCode


/** Mapper between [[ErrorStatus]] and [[StatusCode]]. */
object ErrorStatusCodeMapper:
  /** Converts [[ErrorStatus]] to corresponding [[StatusCode]].
   *
   *  @param err application error.
   *  @return corresponding status code.
   */
  private def toStatusCode(err: ErrorStatus): StatusCode = err match
    // Should br 499 by Google's AIP, but it's not a standard code.
    case Cancelled          => StatusCode.RequestTimeout
    case Unknown            => StatusCode.InternalServerError
    case InvalidArgument    => StatusCode.BadRequest
    case DeadlineExceeded   => StatusCode.GatewayTimeout
    case NotFound           => StatusCode.NotFound
    case AlreadyExists      => StatusCode.Conflict
    case PermissionDenied   => StatusCode.Forbidden
    case Unauthenticated    => StatusCode.Unauthorized
    case ResourceExhausted  => StatusCode.TooManyRequests
    case FailedPrecondition => StatusCode.BadRequest
    case Aborted            => StatusCode.Conflict
    case OutOfRange         => StatusCode.BadRequest
    case Unimplemented      => StatusCode.NotImplemented
    case Internal           => StatusCode.InternalServerError
    case Unavailable        => StatusCode.ServiceUnavailable
    case DataLoss           => StatusCode.InternalServerError

  /** Returns response itself with corresponding error code.
   *  @param response error response from application layer.
   */
  def toApiResponse(
      response: ErrorResponse,
  ): (StatusCode, ErrorResponse) = (toStatusCode(response.status), response)
