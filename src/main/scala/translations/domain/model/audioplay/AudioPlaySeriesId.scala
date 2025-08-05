package org.aulune
package translations.domain.model.audioplay

opaque type AudioPlaySeriesId = Long


object AudioPlaySeriesId:
  def apply(id: Long): AudioPlaySeriesId = id

  extension (id: AudioPlaySeriesId) def value: Long = id
