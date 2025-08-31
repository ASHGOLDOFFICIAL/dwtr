package org.aulune.aggregator
package adapters.service.mappers


import application.dto.audioplay.AudioPlaySeriesResource
import domain.model.audioplay.AudioPlaySeries


/** Mapper between external [[AudioPlaySeriesResource]] and domain's
 *  [[AudioPlaySeries]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object AudioPlaySeriesMapper:
  /** Convert [[AudioPlaySeries]] to [[AudioPlaySeriesResource]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def toResponse(domain: AudioPlaySeries): AudioPlaySeriesResource =
    AudioPlaySeriesResource(id = domain.id, name = domain.name)
