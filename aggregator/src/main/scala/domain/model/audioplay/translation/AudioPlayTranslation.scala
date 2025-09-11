package org.aulune.aggregator
package domain.model.audioplay.translation


import domain.errors.TranslationValidationError
import domain.model.audioplay.AudioPlay
import domain.model.audioplay.translation.AudioPlayTranslation.ValidationResult
import domain.model.shared.{
  ExternalResource,
  Language,
  SelfHostedLocation,
  TranslatedTitle,
}

import cats.data.{NonEmptyList, Validated, ValidatedNec}
import cats.syntax.all.given
import org.aulune.commons.types.Uuid

import java.net.URI


/** Audio play translation representation.
 *  @param originalId original work's ID.
 *  @param id translation ID.
 *  @param title translated title.
 *  @param translationType translation type.
 *  @param language translation language.
 *  @param selfHostedLocation link to self-hosted place where this translation
 *    can be consumed.
 *  @param externalResources links to different resources.
 */
final case class AudioPlayTranslation private (
    originalId: Uuid[AudioPlay],
    id: Uuid[AudioPlayTranslation],
    title: TranslatedTitle,
    translationType: AudioPlayTranslationType,
    language: Language,
    selfHostedLocation: Option[SelfHostedLocation],
    externalResources: List[ExternalResource],
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
      selfHostedLocation: Option[SelfHostedLocation] = selfHostedLocation,
      externalResources: List[ExternalResource] = externalResources,
  ): ValidationResult[AudioPlayTranslation] = AudioPlayTranslation(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    selfHostedLocation = selfHostedLocation,
    externalResources = externalResources,
  )


object AudioPlayTranslation:
  private type ValidationResult[A] = ValidatedNec[TranslationValidationError, A]

  /** Creates an audio play translation with state validation.
   *  @param originalId original work ID.
   *  @param id ID.
   *  @param title translated title.
   *  @param translationType translation type.
   *  @param language translation language.
   *  @param selfHostedLocation link to self-hosted place where this translation
   *    can be consumed.
   *  @param externalResources links to different resources.
   *  @return translation validation result.
   */
  def apply(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
      title: TranslatedTitle,
      translationType: AudioPlayTranslationType,
      language: Language,
      selfHostedLocation: Option[SelfHostedLocation],
      externalResources: List[ExternalResource],
  ): ValidationResult[AudioPlayTranslation] = validateState(
    new AudioPlayTranslation(
      originalId = originalId,
      id = id,
      title = title,
      translationType = translationType,
      language = language,
      selfHostedLocation = selfHostedLocation,
      externalResources = externalResources,
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
      selfHostedLocation: Option[SelfHostedLocation],
      externalResources: List[ExternalResource],
  ): AudioPlayTranslation = AudioPlayTranslation(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    selfHostedLocation = selfHostedLocation,
    externalResources = externalResources,
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
