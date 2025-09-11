package org.aulune.commons
package adapters.circe


import adapters.circe.CirceUtils.config
import errors.{ErrorDetails, ErrorInfo, ErrorReason, ErrorResponse, ErrorStatus}

import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.{Decoder, Encoder}

import scala.util.Failure


/** [[Decoder]] and [[Encoder]] instances for [[ErrorResponse]]. */
object ErrorResponseCodecs:
  given Decoder[ErrorResponse] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  given Encoder[ErrorResponse] = deriveConfiguredEncoder

  private given Decoder[ErrorStatus] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  private given Encoder[ErrorStatus] = Encoder.encodeInt.contramap(_.value)

  private given Decoder[ErrorDetails] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  private given Encoder[ErrorDetails] = deriveConfiguredEncoder

  private given Decoder[ErrorInfo] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  private given Encoder[ErrorInfo] = deriveConfiguredEncoder

  private given Decoder[ErrorReason] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  private given Encoder[ErrorReason] = Encoder.encodeString.contramap(_.name)
