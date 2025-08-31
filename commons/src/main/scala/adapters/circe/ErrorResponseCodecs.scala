package org.aulune.commons
package adapters.circe

import errors.{ErrorInfo, ErrorReason, ErrorResponse, ErrorStatus}
import CirceUtils.config

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}

import scala.util.Failure


/** [[Decoder]] and [[Encoder]] instances for [[ErrorResponse]]. */
object ErrorResponseCodecs:
  given Decoder[ErrorResponse] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  given Encoder[ErrorResponse] = deriveConfiguredEncoder

  private given Decoder[ErrorStatus] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  private given Encoder[ErrorStatus] = deriveConfiguredEncoder

  private given Decoder[ErrorInfo] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  private given Encoder[ErrorInfo] = deriveConfiguredEncoder

  private given Decoder[ErrorReason] = Decoder.decodeString
    .emapTry(_ => Failure(new UnsupportedOperationException()))
  private given Encoder[ErrorReason] = Encoder.encodeString.contramap(_.name)
