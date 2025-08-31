package org.aulune.commons
package adapters.tapir

import errors.{ErrorDetails, ErrorInfo, ErrorReason, ErrorResponse, ErrorStatus}

import sttp.tapir.SchemaType.{SInteger, SString}
import sttp.tapir.{Schema, Validator}


/** Tapir schemas for [[ErrorResponse]]. */
object ErrorResponseSchemas:
  given Schema[ErrorResponse] = Schema.derived

  private given Schema[ErrorStatus] = Schema(
    schemaType = SInteger(),
  )
  private given Schema[ErrorDetails] = Schema.derived
  private given Schema[ErrorInfo] = Schema.derived
  private given Schema[ErrorReason] = Schema(
    schemaType = SString(),
  )
