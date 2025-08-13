package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.model.audioplay.{
  AudioPlaySeriesNumber,
  AudioPlayTitle
}
import translations.domain.shared.ExternalResourceType

import doobie.Meta


private[postgres] object AudioPlayMetas:
  given Meta[AudioPlayTitle] = Meta[String].tiemap { str =>
    AudioPlayTitle(str)
      .toRight(s"Failed to decode AudioPlayTitle from: $str.")
  }(identity)

  given Meta[AudioPlaySeriesNumber] = Meta[Int].tiemap { str =>
    AudioPlaySeriesNumber(str).toRight(
      s"Failed to decode AudioPlaySeriesNumber from: $str.")
  }(identity)

  given Meta[ExternalResourceType] =
    val toInt: Map[ExternalResourceType, Int] =
      ExternalResourceType.values.map {
        case t @ ExternalResourceType.Purchase  => t -> 1
        case t @ ExternalResourceType.Streaming => t -> 2
        case t @ ExternalResourceType.Download  => t -> 3
        case t @ ExternalResourceType.Other     => t -> 4
        case t @ ExternalResourceType.Private   => t -> 5
      }.toMap
    val fromInt = toInt.map(_.swap)
    Meta[Int].timap(fromInt.apply)(toInt.apply)
