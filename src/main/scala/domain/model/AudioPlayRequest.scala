package org.aulune
package domain.model


case class AudioPlayRequest(
    title: String,
    seriesId: Option[Long],
    seriesOrder: Option[Int],
):
  def toDomain(id: MediaResourceID): AudioPlay = AudioPlay(
    id = id,
    title = AudioPlayTitle(title),
    seriesId = seriesId.map(AudioPlaySeriesId(_)),
    seriesOrder = seriesOrder,
  )
