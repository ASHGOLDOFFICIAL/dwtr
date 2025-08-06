package org.aulune
package translations.domain.model.translation

import java.util.UUID

opaque type TranslationId = String


object TranslationId:
  def apply(uuid: UUID): TranslationId = uuid.toString

  def unsafeApply(uuid: String): TranslationId = uuid

  extension (id: TranslationId) def string: String = id
