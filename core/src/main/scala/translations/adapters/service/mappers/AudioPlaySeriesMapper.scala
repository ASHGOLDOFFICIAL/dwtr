package org.aulune
package translations.adapters.service.mappers


import translations.application.dto.audioplay.AudioPlaySeriesResponse
import translations.domain.model.audioplay.AudioPlaySeries


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
