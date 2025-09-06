package org.aulune.aggregator
package domain.model.audioplay.translation


/** Possible audio translation types. */
enum AudioPlayTranslationType:
  /** Translated as document. */
  case Transcript

  /** Synchronized subtitles. */
  case Subtitles

  /** Voice-over. */
  case VoiceOver
