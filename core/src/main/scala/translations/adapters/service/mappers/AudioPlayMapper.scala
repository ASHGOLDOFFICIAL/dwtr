package org.aulune
package translations.adapters.service.mappers


import shared.model.Uuid
import shared.pagination.CursorToken
import translations.application.dto.audioplay.{
  AudioPlayRequest,
  AudioPlayResponse,
  ListAudioPlaysResponse,
}
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.domain.errors.AudioPlayValidationError
import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import translations.domain.model.person.Person
import translations.domain.shared.{ReleaseDate, Synopsis}

import cats.data.ValidatedNec
import cats.syntax.all.given

import java.util.UUID


/** Mapper between external audio plays DTOs and domain's [[AudioPlay]].
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object AudioPlayMapper:
  /** Converts request to domain object and verifies it.
   *  @param request audio play request DTO.
   *  @param id ID assigned to this audio play.
   *  @param series previously fetched by given series ID series (if series ID
   *    was given).
   *  @return created domain object if valid.
   */
  def fromRequest(
      request: AudioPlayRequest,
      id: UUID,
      series: Option[AudioPlaySeries],
  ): ValidatedNec[AudioPlayValidationError, AudioPlay] = (for
    title <- AudioPlayTitle(request.title)
    synopsis <- Synopsis(request.synopsis)
    releaseDate <- ReleaseDate(request.releaseDate)
    writers = request.writers.map(Uuid[Person])
    cast <- request.cast.toList.traverse(CastMemberMapper.toDomain)
    season <- request.seriesSeason.map(AudioPlaySeason.apply)
    number <- request.seriesNumber.map(AudioPlaySeriesNumber.apply)
    resources = request.externalResources.map(ExternalResourceMapper.toDomain)
  yield AudioPlay(
    id = Uuid[AudioPlay](id),
    title = title,
    synopsis = synopsis,
    writers = writers,
    cast = cast,
    releaseDate = releaseDate,
    series = series,
    seriesSeason = season,
    seriesNumber = number,
    coverUrl = None,
    externalResources = resources,
  )).getOrElse(AudioPlayValidationError.InvalidValues.invalidNec)

  /** Converts domain object to response object.
   *  @param domain entity to use as a base.
   */
  def toResponse(domain: AudioPlay): AudioPlayResponse = AudioPlayResponse(
    id = domain.id,
    title = domain.title,
    synopsis = domain.synopsis,
    releaseDate = domain.releaseDate,
    writers = domain.writers,
    cast = domain.cast.map(CastMemberMapper.fromDomain),
    series = domain.series.map(AudioPlaySeriesMapper.toResponse),
    seriesSeason = domain.seriesSeason,
    seriesNumber = domain.seriesNumber,
    coverUrl = domain.coverUrl,
    externalResources = domain.externalResources
      .map(ExternalResourceMapper.fromDomain),
  )

  /** Converts list of domain objects to one list response.
   *  @param audios list of domain objects.
   */
  def toListResponse(audios: List[AudioPlay]): ListAudioPlaysResponse =
    val nextPageToken = audios.lastOption.flatMap { elem =>
      val token = AudioPlayToken(elem.id)
      CursorToken[AudioPlayToken](token).encode
    }
    ListAudioPlaysResponse(audios.map(toResponse), nextPageToken)
