package org.aulune.aggregator
package adapters.service


import domain.model.audioplay.series.{AudioPlaySeries, AudioPlaySeriesName}

import org.aulune.commons.types.Uuid


/** [[AudioPlaySeries]] objects to use in tests. */
private[aggregator] object AudioPlaySeriesStubs:
  /** ''Mega Series'' audio play series. */
  val series1: AudioPlaySeries = AudioPlaySeries
    .unsafe(
      id = Uuid.unsafe("3669ae36-b459-448e-a51e-f8bbc3a41b79"),
      name = AudioPlaySeriesName.unsafe("Mega Series"),
    )

  /** ''Super-soap-drama'' audio play series. */
  val series2: AudioPlaySeries = AudioPlaySeries
    .unsafe(
      id = Uuid.unsafe("8dddf9f1-3f59-41bb-b9b4-c97c861913c2"),
      name = AudioPlaySeriesName.unsafe("Super-soap-drama-series"),
    )

  /** ''Super Series'' audio play series. */
  val series3: AudioPlaySeries = AudioPlaySeries
    .unsafe(
      id = Uuid.unsafe("dfaf0048-7d42-4fe5-b221-aec7aa5da90c"),
      name = AudioPlaySeriesName.unsafe("Super Series"),
    )
