package org.aulune
package aggregator.adapters.service.mappers


import aggregator.application.dto.{
  AudioPlayTranslationListResponse,
  AudioPlayTranslationRequest,
  AudioPlayTranslationResponse
}
import aggregator.application.repositories.TranslationRepository.AudioPlayTranslationCursor
import aggregator.domain.errors.TranslationValidationError
import aggregator.domain.model.audioplay.{AudioPlay, AudioPlayTranslation}
import aggregator.domain.shared.TranslatedTitle
import commons.pagination.CursorEncoder
import commons.types.Uuid

import cats.data.{NonEmptyList, ValidatedNec}
import cats.syntax.all.given


/** Mapper between external audio play translation DTOs and domain's
 *  [[AudioPlayTranslation]].
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object AudioPlayTranslationMapper:
  /** Converts request to domain object and verifies it.
   *  @param request audio play request DTO.
   *  @param originalId ID of original audio play.
   *  @param id ID assigned to this audio play translation.
   *  @return created domain object if valid.
   */
  def fromRequest(
      request: AudioPlayTranslationRequest,
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
  ): ValidatedNec[TranslationValidationError, AudioPlayTranslation] = (for
    title <- TranslatedTitle(request.title)
    translationType = AudioPlayTranslationTypeMapper
      .toDomain(request.translationType)
    language = LanguageMapper.toDomain(request.language)
    links <- NonEmptyList.fromList(request.links)
  yield AudioPlayTranslation(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    links = links,
  )).getOrElse(TranslationValidationError.InvalidArguments.invalidNec)

  /** Converts domain object to response object.
   *  @param domain entity to use as a base.
   */
  def toResponse(domain: AudioPlayTranslation): AudioPlayTranslationResponse =
    AudioPlayTranslationResponse(
      originalId = domain.originalId,
      id = domain.id,
      title = domain.title,
      translationType = AudioPlayTranslationTypeMapper
        .fromDomain(domain.translationType),
      language = LanguageMapper.fromDomain(domain.language),
      links = domain.links.toList,
    )

  /** Converts list of domain objects to one list response.
   *  @param translations list of domain objects.
   */
  def toListResponse(
      translations: List[AudioPlayTranslation],
  ): AudioPlayTranslationListResponse =
    val nextPageToken = translations.lastOption.map { elem =>
      val cursor = AudioPlayTranslationCursor(elem.originalId, elem.id)
      CursorEncoder[AudioPlayTranslationCursor].encode(cursor)
    }
    val elements = translations.map(toResponse)
    AudioPlayTranslationListResponse(elements, nextPageToken)
