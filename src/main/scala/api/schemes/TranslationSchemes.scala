package org.aulune
package api.schemes


import domain.model.{TranslationId, TranslationTitle}

import sttp.tapir.Schema

import java.net.URI


object TranslationSchemes:
  given Schema[URI]              = Schema.string[URI]
  given Schema[TranslationId]    = Schema.schemaForLong.as[TranslationId]
  given Schema[TranslationTitle] = Schema.schemaForString.as[TranslationTitle]
