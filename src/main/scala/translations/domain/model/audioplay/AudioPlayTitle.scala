package org.aulune
package translations.domain.model.audioplay

opaque type AudioPlayTitle = String


object AudioPlayTitle:
  def apply(value: String): AudioPlayTitle = value

  extension (title: AudioPlayTitle) def value: String = title
