package org.aulune.aggregator
package application.dto


/** Values to be used as translation type. */
enum AudioPlayTranslationTypeDto:
  /** Translated as document. */
  case Transcript

  /** Synchronized subtitles. */
  case Subtitles

  /** Voice-over. */
  case VoiceOver
