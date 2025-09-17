package org.aulune.aggregator
package api.http.tapir


import api.mappers.{
  DateAccuracyMapper,
  ExternalResourceTypeMapper,
  LanguageMapper,
}
import application.dto.shared.ReleaseDateDTO.DateAccuracyDTO
import application.dto.shared.{
  ExternalResourceDTO,
  ExternalResourceTypeDTO,
  LanguageDTO,
  ReleaseDateDTO,
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

  given Schema[LanguageDTO] = Schema.string
    .validate(
      Validator
        .enumeration(LanguageDTO.values.toList)
        .encode(LanguageMapper.toString))

  given Schema[ReleaseDateDTO] = Schema.derived
  private given Schema[DateAccuracyDTO] = Schema.string
    .validate(
      Validator
        .enumeration(DateAccuracyDTO.values.toList)
        .encode(DateAccuracyMapper.toString))
