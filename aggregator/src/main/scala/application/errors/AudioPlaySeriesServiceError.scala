package org.aulune.aggregator
package application.errors


import application.AudioPlaySeriesService

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur in [[AudioPlaySeriesService]].
 *  @param reason string representation of error.
 */
enum AudioPlaySeriesServiceError(val reason: String)
    extends ErrorReason(reason):
  /** No audio play series with given ID is found. */
  case SeriesNotFound extends AudioPlaySeriesServiceError("SERIES_NOT_FOUND")

  /** Given audio play series is not valid. */
  case InvalidSeries extends AudioPlaySeriesServiceError("INVALID_SERIES")
