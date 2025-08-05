package org.aulune
package api.dto


import domain.model.{AudioPlay, MediaResourceId}

import java.util.UUID


case class AudioPlayResponse(
    name: String,
    id: String,
    title: String,
    seriesId: Option[Long],
    seriesOrder: Option[Int],
)


object AudioPlayResponse:
  def fromDomain(domain: AudioPlay): AudioPlayResponse = AudioPlayResponse(
    name = name(domain),
    id = domain.id.string,
    title = domain.title.value,
    seriesId = domain.seriesId.map(_.value),
    seriesOrder = domain.seriesOrder,
  )

  inline val collectionIdentifier: "audioplays" = "audioplays"

  def name(domain: AudioPlay): String =
    s"$collectionIdentifier/${domain.id}"
