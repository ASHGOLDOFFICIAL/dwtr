package org.aulune.aggregator
package application.dto.audioplay.translation


/** Values to be used as translation type. */
enum AudioPlayTranslationTypeDTO:
  /** Translated as document. */
  case Transcript

  /** Synchronized subtitles. */
  case Subtitles

  /** Voice-over. */
  case VoiceOver
