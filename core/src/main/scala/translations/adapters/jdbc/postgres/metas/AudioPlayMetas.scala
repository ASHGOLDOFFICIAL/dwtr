package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.model.audioplay.{AudioPlaySeason, AudioPlaySeriesNumber, AudioPlayTitle}
import translations.domain.shared.ExternalResourceType

import doobie.Meta
import doobie.postgres.implicits.*


private[postgres] object AudioPlayMetas:
  given Meta[AudioPlayTitle] = Meta[String].tiemap { str =>
    AudioPlayTitle(str)
      .toRight(s"Failed to decode AudioPlayTitle from: $str.")
  }(identity)

  given Meta[AudioPlaySeason] = Meta[Int].tiemap { str =>
    AudioPlaySeason(str).toRight(
      s"Failed to decode AudioPlaySeason from: $str.")
  }(identity)

  given Meta[AudioPlaySeriesNumber] = Meta[Int].tiemap { str =>
    AudioPlaySeriesNumber(str).toRight(
      s"Failed to decode AudioPlaySeriesNumber from: $str.")
  }(identity)

  private val resourceTypeToInt = ExternalResourceType.values.map {
    case t @ ExternalResourceType.Purchase  => t -> 1
    case t @ ExternalResourceType.Streaming => t -> 2
    case t @ ExternalResourceType.Download  => t -> 3
    case t @ ExternalResourceType.Other     => t -> 4
    case t @ ExternalResourceType.Private   => t -> 5
  }.toMap
  private val resourceTypeFromInt = resourceTypeToInt.map(_.swap)

  given Meta[ExternalResourceType] = Meta[Int]
    .timap(resourceTypeFromInt.apply)(resourceTypeToInt.apply)

  given Meta[Array[ExternalResourceType]] = Meta[Array[Int]]
    .timap(_.map(resourceTypeFromInt.apply))(_.map(resourceTypeToInt.apply))
