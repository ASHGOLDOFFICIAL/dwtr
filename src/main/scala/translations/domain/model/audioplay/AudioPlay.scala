package org.aulune
package translations.domain.model.audioplay

import translations.domain.model.shared.MediaResourceId

import java.time.Instant


final case class AudioPlay(
    id: MediaResourceId,
    title: AudioPlayTitle,
    seriesId: Option[AudioPlaySeriesId],
    seriesOrder: Option[Int],
    addedAt: Instant
)
