package org.aulune
package shared


import shared.errors.ApplicationServiceError
import shared.repositories.RepositoryError

import org.aulune
import sttp.model.StatusCode


def toErrorResponse(
    err: ApplicationServiceError
): (StatusCode, String) = err match
  case ApplicationServiceError.BadRequest =>
    (StatusCode.BadRequest, "Bad request")
  case ApplicationServiceError.AlreadyExists =>
    (StatusCode.Conflict, "Already exists")
  case ApplicationServiceError.NotFound => (StatusCode.NotFound, "Not found")
  case ApplicationServiceError.PermissionDenied =>
    (StatusCode.Forbidden, "Permission denied")
  case ApplicationServiceError.InternalError =>
    (StatusCode.InternalServerError, "Internal error")


def toApplicationError(
    err: RepositoryError
): ApplicationServiceError = err match
  case RepositoryError.AlreadyExists  => ApplicationServiceError.AlreadyExists
  case RepositoryError.NotFound       => ApplicationServiceError.NotFound
  case RepositoryError.StorageFailure => ApplicationServiceError.InternalError
