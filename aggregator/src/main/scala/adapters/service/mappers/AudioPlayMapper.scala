package org.aulune.aggregator
package adapters.service.mappers


import application.dto.audioplay.series.AudioPlaySeriesResource
import application.dto.audioplay.{AudioPlayResource, CreateAudioPlayRequest}
import application.dto.person.PersonResource
import domain.errors.AudioPlayValidationError
import domain.errors.AudioPlayValidationError.{
  InvalidCast,
  InvalidReleaseDate,
  InvalidSeason,
  InvalidSelfHostedLocation,
  InvalidSeriesNumber,
  InvalidSynopsis,
  InvalidTitle,
}
import domain.model.audioplay.series.AudioPlaySeries
import domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
  EpisodeType,
}
import domain.model.person.Person
import domain.model.shared.{
  ExternalResource,
  ImageUri,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.given
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
  ): ValidatedNec[AudioPlayValidationError, AudioPlay] = (
    Uuid[AudioPlay](id).validNec,
    AudioPlayTitle(request.title)
      .toValidNec(InvalidTitle),
    Synopsis(request.synopsis)
      .toValidNec(InvalidSynopsis),
    ReleaseDate(request.releaseDate)
      .toValidNec(InvalidReleaseDate),
    request.writers.map(Uuid[Person]).validNec,
    request.cast
      .traverse(CastMemberMapper.fromDTO)
      .toValidNec(InvalidCast),
    request.seriesId.map(Uuid[AudioPlaySeries]).validNec,
    request.seriesSeason
      .traverse(AudioPlaySeason(_).toValidNec(InvalidSeason)),
    request.seriesNumber
      .traverse(AudioPlaySeriesNumber(_).toValidNec(InvalidSeriesNumber)),
    request.episodeType.map(EpisodeTypeMapper.toDomain).validNec,
    Option.empty[ImageUri].validNec,
    request.selfHostedLocation
      .traverse(SelfHostedLocation(_).toValidNec(InvalidSelfHostedLocation)),
    request.externalResources.map(ExternalResourceMapper.toDomain).validNec,
  ).mapN(AudioPlay.apply).andThen(identity)

  /** Converts domain object to response object.
   *
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
