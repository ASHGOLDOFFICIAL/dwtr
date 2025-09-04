package org.aulune.aggregator
package application.errors


import application.AudioPlayService

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur in [[AudioPlayService]].
 *  @param reason string representation of error.
 */
enum AudioPlayServiceError(val reason: String) extends ErrorReason(reason):
  /** Specified audio play series is not found. */
  case AudioPlaySeriesNotFound
      extends AudioPlayServiceError("AUDIO_PLAY_SERIES_NOT_FOUND")

  /** No audio play with given ID is found. */
  case AudioPlayNotFound extends AudioPlayServiceError("AUDIO_PLAY_NOT_FOUND")

  /** Given audio play is not valid audio play. */
  case InvalidAudioPlay extends AudioPlayServiceError("INVALID_AUDIO_PLAY")
