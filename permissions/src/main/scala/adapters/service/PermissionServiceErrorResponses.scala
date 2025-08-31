package org.aulune.permissions
package adapters.service


import application.errors.PermissionServiceError
import application.errors.PermissionServiceError.{
  InvalidPermission,
  PermissionNotFound,
}

import org.aulune.commons.errors.ErrorStatus.{
  FailedPrecondition,
  Internal,
  InvalidArgument,
}
import org.aulune.commons.errors.{ErrorInfo, ErrorResponse}


/** Error responses for [[PermissionServiceImpl]]. */
object PermissionServiceErrorResponses:
  private val domain = "org.aulune.permissions"

  val internal: ErrorResponse = ErrorResponse(
    status = Internal,
    message = "Internal error.",
    details = Nil,
  )

  val invalidPermission: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Given permission is not valid.",
    details = List(
      ErrorInfo(
        reason = InvalidPermission,
        domain = domain,
      )),
  )

  val unregisteredPermission: ErrorResponse = ErrorResponse(
    status = FailedPrecondition,
    message = "Asked permission isn't registered yet.",
    details = List(
      ErrorInfo(
        reason = PermissionNotFound,
        domain = domain,
      ),
    ),
  )
