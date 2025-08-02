package org.aulune
package domain.model

case class AudioPlaySeriesId(value: Long)      extends AnyVal
case class AudioPlaySeriesTitle(value: String) extends AnyVal

case class AudioPlaySeries(id: AudioPlaySeriesId, title: AudioPlaySeriesTitle)
