package org.aulune
package domain.model

import java.time.Instant


case class AudioPlay(
    id: MediaResourceId,
    title: AudioPlayTitle,
    seriesId: Option[AudioPlaySeriesId],
    seriesOrder: Option[Int],
    addedAt: Instant
)
