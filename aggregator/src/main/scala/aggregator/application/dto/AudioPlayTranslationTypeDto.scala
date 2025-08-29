package org.aulune
package aggregator.application.dto

/** Values to be used as translation type. */
enum AudioPlayTranslationTypeDto:
  /** Translated as document. */
  case Transcript

  /** Synchronized subtitles. */
  case Subtitles

  /** Voice-over. */
  case VoiceOver
