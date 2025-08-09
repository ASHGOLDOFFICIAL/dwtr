package org.aulune
package translations.domain.model.audioplay

import translations.domain.errors.TranslationValidationError
import translations.domain.errors.TranslationValidationError.*
import translations.domain.shared.{TranslatedTitle, Uuid}

import cats.data.{NonEmptyList, ValidatedNec}
import cats.syntax.all.*

import java.net.URI
import java.time.Instant
import java.util.UUID


/** Audio play translation representation.
 * @param originalId original work's ID.
 * @param id translation ID.
 * @param title translated title.
 * @param links publication links.
 * @param addedAt when translation was added.
 */
final case class AudioPlayTranslation private (
    originalId: Uuid[AudioPlay],
    id: Uuid[AudioPlayTranslation],
    title: TranslatedTitle,
    links: NonEmptyList[URI],
    addedAt: Instant,
)


object AudioPlayTranslation:
  private type ValidationResult[A] = ValidatedNec[TranslationValidationError, A]

  /** Creates an audio play with state validation.
   *  @param id ID.
   *  @param title translated title.
   *  @param originalId original work ID.
   *  @param links publications.
   *  @param addedAt when it was added.
   *  @return translation validation result.
   */
  def apply(
      id: UUID,
      title: String,
      originalId: UUID,
      addedAt: Instant,
      links: List[URI],
  ): ValidationResult[AudioPlayTranslation] = (
    Uuid[AudioPlay](id).validNec,
    Uuid[AudioPlayTranslation](id).validNec,
    TranslatedTitle(title).toValidNec(InvalidTitle),
    validateLinks(links),
    addedAt.validNec,
  ).mapN(new AudioPlayTranslation(_, _, _, _, _))

  /** Returns updated translation.
   *  @param initial initial state.
   *  @param title new title.
   *  @param links publications.
   *  @return new state validation result.
   *  @note Other fields are not supposed to be updated, use [[apply]] instead
   *    to create new instance.
   */
  def update(
      initial: AudioPlayTranslation,
      title: String,
      links: List[URI],
  ): ValidationResult[AudioPlayTranslation] = (
    initial.originalId.validNec,
    initial.id.validNec,
    TranslatedTitle(title).toValidNec(InvalidTitle),
    validateLinks(links),
    initial.addedAt.validNec,
  ).mapN(new AudioPlayTranslation(_, _, _, _, _))

  /** Validates links. Non empty list is required.
   *  @param links publications.
   *  @return validation result.
   */
  private def validateLinks(
      links: List[URI],
  ): ValidationResult[NonEmptyList[URI]] =
    NonEmptyList.fromList(links).toValidNec(InvalidLinks)
