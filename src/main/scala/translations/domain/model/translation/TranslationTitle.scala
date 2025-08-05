package org.aulune
package translations.domain.model.translation

opaque type TranslationTitle = String


object TranslationTitle:
  def apply(value: String): TranslationTitle = value

  extension (title: TranslationTitle) def value: String = title
