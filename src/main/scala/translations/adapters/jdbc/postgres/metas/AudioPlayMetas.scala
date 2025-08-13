package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.model.audioplay.{
  AudioPlaySeriesNumber,
  AudioPlayTitle
}

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
