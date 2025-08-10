package org.aulune
package translations.domain.model.audioplay


/** Possible audio translation types. */
enum AudioPlayTranslationType:
  /** Translated as document. */
  case Transcript

  /** Synchronized subtitles. */
  case Subtitles

  /** Voice-over. */
  case VoiceOver
