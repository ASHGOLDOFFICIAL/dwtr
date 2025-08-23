package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeriesName,
  AudioPlaySeriesNumber,
  AudioPlayTitle
}
import translations.domain.shared.ExternalResourceType

import doobie.Meta
import doobie.postgres.implicits.unliftedUnboxedIntegerArrayType


/** [[Meta]] instances for [[AudioPlay]]. */
private[postgres] object AudioPlayMetas:
  given Meta[AudioPlayTitle] = Meta[String]
    .imap(AudioPlayTitle.unsafe)(identity)

  given Meta[AudioPlaySeason] = Meta[Int]
    .imap(AudioPlaySeason.unsafe)(identity)

  given Meta[AudioPlaySeriesName] = Meta[String]
    .imap(AudioPlaySeriesName.unsafe)(identity)

  given Meta[AudioPlaySeriesNumber] = Meta[Int]
    .imap(AudioPlaySeriesNumber.unsafe)(identity)

  private val resourceTypeToInt = ExternalResourceType.values.map {
    case t @ ExternalResourceType.Purchase  => t -> 1
    case t @ ExternalResourceType.Streaming => t -> 2
    case t @ ExternalResourceType.Download  => t -> 3
    case t @ ExternalResourceType.Other     => t -> 4
    case t @ ExternalResourceType.Private   => t -> 5
  }.toMap
  private val resourceTypeFromInt = resourceTypeToInt.map(_.swap)

  given Meta[ExternalResourceType] = Meta[Int]
    .imap(resourceTypeFromInt.apply)(resourceTypeToInt.apply)

  given Meta[Array[ExternalResourceType]] = Meta[Array[Int]]
    .imap(_.map(resourceTypeFromInt.apply))(_.map(resourceTypeToInt.apply))
