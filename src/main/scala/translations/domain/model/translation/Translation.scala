package org.aulune
package translations.domain.model.translation


import translations.domain.errors.TranslationValidationError
import translations.domain.errors.TranslationValidationError.*
import translations.domain.model.audioplay.AudioPlay
import translations.domain.model.shared.Uuid

import cats.data.{NonEmptyList, ValidatedNec}
import cats.syntax.all.*

import java.net.URI
import java.time.Instant
import java.util.UUID


final case class Translation private (
    id: Uuid[Translation],
    title: TranslatedTitle,
    originalId: Uuid[AudioPlay],
    addedAt: Instant,
    links: NonEmptyList[URI],
)


object Translation:
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
  ): ValidationResult[Translation] = (
    Uuid[Translation](id).validNec,
    TranslatedTitle(title).toValidNec(InvalidTitle),
    Uuid[AudioPlay](id).validNec,
    addedAt.validNec,
    validateLinks(links),
  ).mapN(new Translation(_, _, _, _, _))

  /** Returns updated translation.
   *  @param initial initial state.
   *  @param title new title.
   *  @param links publications.
   *  @return new state validation result.
   *  @note Other fields are not supposed to be updated, use [[apply]] instead
   *    to create new instance.
   */
  def update(
      initial: Translation,
      title: String,
      links: List[URI],
  ): ValidationResult[Translation] = (
    initial.id.validNec,
    TranslatedTitle(title).toValidNec(InvalidTitle),
    initial.originalId.validNec,
    initial.addedAt.validNec,
    validateLinks(links),
  ).mapN(new Translation(_, _, _, _, _))

  /** Validates links. Non empty list is required.
   *  @param links publications.
   *  @return validation result.
   */
  private def validateLinks(
      links: List[URI],
  ): ValidationResult[NonEmptyList[URI]] =
    NonEmptyList.fromList(links).toValidNec(InvalidLinks)
