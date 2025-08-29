package org.aulune
package translations.domain.model.audioplay

import shared.model.Uuid
import translations.domain.errors.TranslationValidationError.*
import translations.domain.errors.{
  AudioPlayValidationError,
  TranslationValidationError,
}
import translations.domain.model.audioplay.AudioPlayTranslation.{
  ValidationResult,
  validateState,
}
import translations.domain.shared.{Language, TranslatedTitle}

import cats.data.{NonEmptyList, Validated, ValidatedNec}
import cats.syntax.all.*

import java.net.URI
import java.time.Instant
import java.util.UUID


/** Audio play translation representation.
 *  @param originalId original work's ID.
 *  @param id translation ID.
 *  @param title translated title.
 *  @param translationType translation type.
 *  @param language translation language.
 *  @param links publication links.
 */
final case class AudioPlayTranslation private (
    originalId: Uuid[AudioPlay],
    id: Uuid[AudioPlayTranslation],
    title: TranslatedTitle,
    translationType: AudioPlayTranslationType,
    language: Language,
    links: NonEmptyList[URI],
):

  /** Copies with validation.
   *  @return new state validation result.
   */
  def update(
      originalId: Uuid[AudioPlay] = originalId,
      id: Uuid[AudioPlayTranslation] = id,
      title: TranslatedTitle = title,
      translationType: AudioPlayTranslationType = translationType,
      language: Language = language,
      links: NonEmptyList[URI] = links,
  ): ValidationResult[AudioPlayTranslation] = validateState(
    new AudioPlayTranslation(
      originalId = originalId,
      id = id,
      title = title,
      translationType = translationType,
      language = language,
      links = links,
    ))


object AudioPlayTranslation:
  private type ValidationResult[A] = ValidatedNec[TranslationValidationError, A]

  /** Creates an audio play translation with state validation.
   *  @param originalId original work ID.
   *  @param id ID.
   *  @param title translated title.
   *  @param translationType translation type.
   *  @param language translation language.
   *  @param links publications.
   *  @return translation validation result.
   */
  def apply(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
      title: TranslatedTitle,
      translationType: AudioPlayTranslationType,
      language: Language,
      links: NonEmptyList[URI],
  ): ValidationResult[AudioPlayTranslation] = validateState(
    new AudioPlayTranslation(
      originalId = originalId,
      id = id,
      title = title,
      translationType = translationType,
      language = language,
      links = links,
    ))

  /** Unsafe constructor to use only inside always-valid boundary.
   *  @throws TranslationValidationError if constructs invalid object.
   */
  def unsafe(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
      title: TranslatedTitle,
      translationType: AudioPlayTranslationType,
      language: Language,
      links: NonEmptyList[URI],
  ): AudioPlayTranslation = AudioPlayTranslation(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    links = links,
  ) match
    case Validated.Valid(a)   => a
    case Validated.Invalid(e) => throw e.head

  /** Validates audio play translation state.
   *  @param translation audio play translation.
   *  @return validation result.
   */
  private def validateState(
      translation: AudioPlayTranslation,
  ): ValidationResult[AudioPlayTranslation] = translation.validNec
