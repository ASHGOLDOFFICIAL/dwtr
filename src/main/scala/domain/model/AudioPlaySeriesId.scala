package org.aulune
package domain.model

opaque type AudioPlaySeriesId = Long


object AudioPlaySeriesId:
  def apply(id: Long): AudioPlaySeriesId = id

  extension (id: AudioPlaySeriesId) def value: Long = id
