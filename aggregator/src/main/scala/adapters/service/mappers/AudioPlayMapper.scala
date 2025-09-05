package org.aulune.aggregator
package adapters.service.mappers


import application.dto.audioplay.{
  AudioPlayResource,
  CreateAudioPlayRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysResponse
}
import application.dto.person.PersonResource
import domain.errors.AudioPlayValidationError
import domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import domain.model.person.Person
import domain.repositories.AudioPlayRepository.AudioPlayCursor
import domain.shared.{ReleaseDate, Synopsis}

import cats.data.ValidatedNec
import cats.syntax.all.given
import org.aulune.commons.pagination.CursorEncoder
import org.aulune.commons.types.Uuid

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
      request: CreateAudioPlayRequest,
      id: UUID,
      series: Option[AudioPlaySeries],
  ): ValidatedNec[AudioPlayValidationError, AudioPlay] = (for
    title <- AudioPlayTitle(request.title)
    synopsis <- Synopsis(request.synopsis)
    releaseDate <- ReleaseDate(request.releaseDate)
    writers = request.writers.map(Uuid[Person])
    cast <- request.cast.traverse(CastMemberMapper.fromDTO)
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
  )).getOrElse(AudioPlayValidationError.InvalidArguments.invalidNec)

  /** Converts domain object to response object.
   *  @param domain entity to use as a base.
   *  @param personMap map between ID and persons to populate writers and cast.
   *  @note Can throw if function doesn't yield result for some required IDs.
   */
  def toResponse(
      domain: AudioPlay,
      personMap: UUID => PersonResource,
  ): AudioPlayResource = AudioPlayResource(
    id = domain.id,
    title = domain.title,
    synopsis = domain.synopsis,
    releaseDate = domain.releaseDate,
    writers = domain.writers.map(personMap),
    cast = domain.cast.map(p => CastMemberMapper.toResource(p, personMap(p.actor))),
    series = domain.series.map(AudioPlaySeriesMapper.toResponse),
    seriesSeason = domain.seriesSeason,
    seriesNumber = domain.seriesNumber,
    coverUri = domain.coverUri,
    externalResources = domain.externalResources
      .map(ExternalResourceMapper.fromDomain),
  )

  /** Converts list of domain objects to one list response.
   *  @param audios list of domain objects.
   *  @param personMap map between ID and persons to populate writers and cast.
   *  @note Can throw if function doesn't yield result for some required IDs.
   */
  def toListResponse(
      audios: List[AudioPlay],
      personMap: UUID => PersonResource,
  ): ListAudioPlaysResponse =
    val nextPageToken = audios.lastOption.map { elem =>
      val cursor = AudioPlayCursor(elem.id)
      CursorEncoder[AudioPlayCursor].encode(cursor)
    }
    ListAudioPlaysResponse(audios.map(toResponse(_, personMap)), nextPageToken)

  /** Converts list of domain objects to one search response.
   *  @param audios list of domain objects.
   *  @param personMap map between ID and persons to populate writers and cast.
   *  @note Can throw if function doesn't yield result for some required IDs.
   */
  def toSearchResponse(
      audios: List[AudioPlay],
      personMap: UUID => PersonResource,
  ): SearchAudioPlaysResponse =
    SearchAudioPlaysResponse(audios.map(toResponse(_, personMap)))
