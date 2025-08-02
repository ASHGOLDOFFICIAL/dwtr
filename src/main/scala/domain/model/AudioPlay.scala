package org.aulune
package domain.model

case class AudioPlayTitle(value: String) extends AnyVal

case class AudioPlay(
    id: MediaResourceID,
    title: AudioPlayTitle,
    seriesId: Option[AudioPlaySeriesId],
    seriesOrder: Option[Int]
)
