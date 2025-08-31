package org.aulune.aggregator
package adapters.service.errors


import org.aulune.commons.errors.ErrorStatus.{Internal, InvalidArgument}
import org.aulune.commons.errors.{ErrorDetails, ErrorResponse}


/** Error responses shared among aggregator services. */
trait BaseAggregatorErrorResponses:
  protected val domain = "org.aulune.aggregator"

  val internal: ErrorResponse = ErrorResponse(
    status = Internal,
    message = "Internal error.",
    details = ErrorDetails(),
  )

  val invalidPaginationParams: ErrorResponse = ErrorResponse(
    status = InvalidArgument,
    message = "Given pagination params are invalid.",
    details = ErrorDetails(),
  )
