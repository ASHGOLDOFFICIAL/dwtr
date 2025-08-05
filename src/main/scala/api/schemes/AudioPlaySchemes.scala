package org.aulune
package api.schemes


import domain.model.{AudioPlaySeriesId, AudioPlayTitle, MediaResourceId}

import sttp.tapir.Schema


object AudioPlaySchemes:
  given Schema[MediaResourceId] = Schema.schemaForString.as[MediaResourceId]
  given Schema[AudioPlayTitle]    = Schema.schemaForString.as[AudioPlayTitle]
  given Schema[AudioPlaySeriesId] = Schema.schemaForLong.as[AudioPlaySeriesId]
