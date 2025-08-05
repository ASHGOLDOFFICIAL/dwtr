package org.aulune
package api.http.tapir


import domain.model.*
import domain.model.auth.AuthenticationToken

import sttp.tapir.Schema

import java.net.URI


given Schema[URI] = Schema.string[URI]


given Schema[AuthenticationToken] =
  Schema.schemaForString.as[AuthenticationToken]


given Schema[MediaResourceId]   = Schema.schemaForString.as[MediaResourceId]
given Schema[AudioPlayTitle]    = Schema.schemaForString.as[AudioPlayTitle]
given Schema[AudioPlaySeriesId] = Schema.schemaForLong.as[AudioPlaySeriesId]

given Schema[TranslationId]    = Schema.schemaForLong.as[TranslationId]
given Schema[TranslationTitle] = Schema.schemaForString.as[TranslationTitle]
