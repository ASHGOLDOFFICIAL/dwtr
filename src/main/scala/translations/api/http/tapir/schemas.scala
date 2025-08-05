package org.aulune
package translations.api.http.tapir


import auth.domain.model.AuthenticationToken
import translations.domain.model.audioplay.{AudioPlaySeriesId, AudioPlayTitle}
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.{TranslationId, TranslationTitle}

import sttp.tapir.Schema

import java.net.URI


given Schema[URI] = Schema.string[URI]

given Schema[MediaResourceId]   = Schema.schemaForString.as[MediaResourceId]
given Schema[AudioPlayTitle]    = Schema.schemaForString.as[AudioPlayTitle]
given Schema[AudioPlaySeriesId] = Schema.schemaForLong.as[AudioPlaySeriesId]

given Schema[TranslationId]    = Schema.schemaForLong.as[TranslationId]
given Schema[TranslationTitle] = Schema.schemaForString.as[TranslationTitle]
