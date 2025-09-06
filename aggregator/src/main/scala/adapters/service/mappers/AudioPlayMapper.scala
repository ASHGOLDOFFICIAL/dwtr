package org.aulune.aggregator
package adapters.service.mappers


import application.dto.audioplay.series.AudioPlaySeriesResource
import application.dto.audioplay.{
  AudioPlayResource,
  CreateAudioPlayRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysResponse,
}
import application.dto.person.PersonResource
import domain.errors.AudioPlayValidationError
import domain.model.audioplay.series.AudioPlaySeries
import domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import domain.model.person.Person
import domain.model.shared.{ReleaseDate, Synopsis}
import domain.repositories.AudioPlayRepository.AudioPlayCursor

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
   *  @return created domain object if valid.
   */
  def fromRequest(
      request: CreateAudioPlayRequest,
      id: UUID,
  ): ValidatedNec[AudioPlayValidationError, AudioPlay] = (for
    title <- AudioPlayTitle(request.title)
    synopsis <- Synopsis(request.synopsis)
    releaseDate <- ReleaseDate(request.releaseDate)
    writers = request.writers.map(Uuid[Person])
    cast <- request.cast.traverse(CastMemberMapper.fromDTO)
    season <- request.seriesSeason.map(AudioPlaySeason.apply)
    number <- request.seriesNumber.map(AudioPlaySeriesNumber.apply)
    seriesId = request.seriesId.map(Uuid[AudioPlaySeries])
    resources = request.externalResources.map(ExternalResourceMapper.toDomain)
  yield AudioPlay(
    id = Uuid[AudioPlay](id),
    title = title,
    synopsis = synopsis,
    writers = writers,
    cast = cast,
    releaseDate = releaseDate,
    seriesId = seriesId,
    seriesSeason = season,
    seriesNumber = number,
    coverUrl = None,
    externalResources = resources,
  )).getOrElse(AudioPlayValidationError.InvalidArguments.invalidNec)

  /** Converts domain object to response object.
   *  @param domain entity to use as a base.
   *  @param series prefetched audio play series resource.
   *  @param personMap map between ID and persons to populate writers and cast.
   *  @note Can throw if function doesn't yield result for some required IDs.
   */
  def makeResource(
      domain: AudioPlay,
      series: Option[AudioPlaySeriesResource],
      personMap: UUID => PersonResource,
  ): AudioPlayResource = AudioPlayResource(
    id = domain.id,
    title = domain.title,
    synopsis = domain.synopsis,
    releaseDate = domain.releaseDate,
    writers = domain.writers.map(personMap),
    cast = domain.cast
      .map(p => CastMemberMapper.toResource(p, personMap(p.actor))),
    series = series,
    seriesSeason = domain.seriesSeason,
    seriesNumber = domain.seriesNumber,
    coverUri = domain.coverUri,
    externalResources = domain.externalResources
      .map(ExternalResourceMapper.fromDomain),
  )
