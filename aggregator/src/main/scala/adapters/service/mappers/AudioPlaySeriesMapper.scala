package org.aulune.aggregator
package adapters.service.mappers


import application.dto.audioplay.AudioPlaySeriesResponse
import domain.model.audioplay.AudioPlaySeries


/** Mapper between external [[AudioPlaySeriesResponse]] and domain's
 *  [[AudioPlaySeries]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object AudioPlaySeriesMapper:
  /** Convert [[AudioPlaySeries]] to [[AudioPlaySeriesResponse]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def toResponse(domain: AudioPlaySeries): AudioPlaySeriesResponse =
    AudioPlaySeriesResponse(id = domain.id, name = domain.name)
