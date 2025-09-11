package org.aulune.aggregator
package api.http.tapir


import api.mappers.{ExternalResourceTypeMapper, LanguageMapper}
import application.dto.shared.{
  ExternalResourceDTO,
  ExternalResourceTypeDTO,
  LanguageDTO,
}

import sttp.tapir.{Schema, Validator}

import java.net.URI


/** Tapir [[Schema]]s for shared objects. */
object SharedSchemas:
  given Schema[ExternalResourceDTO] = Schema.derived
  given Schema[ExternalResourceTypeDTO] = Schema.string
    .validate(
      Validator
        .enumeration(ExternalResourceTypeDTO.values.toList)
        .encode(ExternalResourceTypeMapper.toString))
  given Schema[URI] = Schema.string[URI]

  private val languageDescription = "Language of translation."
  given Schema[LanguageDTO] = Schema.string
    .validate(
      Validator
        .enumeration(LanguageDTO.values.toList)
        .encode(LanguageMapper.toString))
    .description(languageDescription)
