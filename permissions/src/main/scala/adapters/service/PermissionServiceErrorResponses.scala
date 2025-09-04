package org.aulune.permissions
package adapters.service


import application.errors.PermissionServiceError
import application.errors.PermissionServiceError.{
  InvalidPermission,
  PermissionNotFound,
}

import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.{
  FailedPrecondition,
  Internal,
  InvalidArgument,
}
import org.aulune.commons.errors.{ErrorDetails, ErrorInfo, ErrorResponse}


/** Error responses for [[PermissionServiceImpl]]. */
object PermissionServiceErrorResponses:
  private val domain = "org.aulune.permissions"

  val internal: ErrorResponse = ErrorResponse(
    status = Internal,
    message = "Internal error.",
    details = ErrorDetails(),
  )

  val invalidPermission: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Given permission is not valid.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = InvalidPermission,
        domain = domain,
      ).some,
    ),
  )

  val unregisteredPermission: ErrorResponse = ErrorResponse(
    status = FailedPrecondition,
    message = "Asked permission isn't registered yet.",
    details = ErrorDetails(
      info = ErrorInfo(
        reason = PermissionNotFound,
        domain = domain,
      ).some,
    ),
  )
