package org.aulune.aggregator
package adapters.service.mappers

import domain.model.audioplay.AudioPlaySeries
import org.aulune.aggregator.application.dto.audioplay.series.AudioPlaySeriesResource


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
