package org.aulune.aggregator
package application.errors


import application.AudioPlayTranslationService

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur in [[AudioPlayTranslationService]].
 *  @param reason string representation of error.
 */
enum TranslationServiceError(val reason: String) extends ErrorReason(reason):
  /** Specified translation is not found. */
  case TranslationNotFound
      extends TranslationServiceError("TRANSLATION_NOT_FOUND")

  /** Given translation is not valid translation. */
  case InvalidTranslation extends TranslationServiceError("INVALID_TRANSLATION")
