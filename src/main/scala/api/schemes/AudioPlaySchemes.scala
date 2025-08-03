package org.aulune
package api.schemes


import domain.model.{AudioPlaySeriesId, AudioPlayTitle}

import sttp.tapir.Schema


object AudioPlaySchemes:
  given Schema[AudioPlayTitle]    = Schema.schemaForString.as[AudioPlayTitle]
  given Schema[AudioPlaySeriesId] = Schema.schemaForLong.as[AudioPlaySeriesId]
