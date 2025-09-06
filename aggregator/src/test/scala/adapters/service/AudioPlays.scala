package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlayServiceErrorResponses
import adapters.service.mappers.AudioPlayMapper
import application.AudioPlayService
import application.dto.audioplay.{
  AudioPlayResource,
  CreateAudioPlayRequest,
  DeleteAudioPlayRequest,
  GetAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
  SearchAudioPlaysResponse,
}
import domain.model.audioplay.{
  ActorRole,
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesName,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import domain.shared.ExternalResourceType.{
  Download,
  Other,
  Private,
  Purchase,
  Streaming,
}
import domain.shared.{ExternalResource, ImageUri, ReleaseDate, Synopsis}

import cats.Applicative
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid

import java.net.URI
import java.time.LocalDate
import java.util.UUID


/** [[AudioPlay]] objects to use in tests. */
private[aggregator] object AudioPlays:
  private def makeCoverUri(url: String): Option[ImageUri] =
    ImageUri.unsafe(URI.create(url)).some

  private def makeReleaseDate(year: Int, month: Int, day: Int): ReleaseDate =
    ReleaseDate.unsafe(LocalDate.of(year, month, day))

  private def makeSeries(uuid: String, name: String): Option[AudioPlaySeries] =
    AudioPlaySeries
      .unsafe(Uuid.unsafe(uuid), AudioPlaySeriesName.unsafe(name))
      .some

  /** ''Magic Mountain'' audio play. */
  val audioPlay1: AudioPlay = AudioPlay.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea66"),
    title = AudioPlayTitle.unsafe("Magic Mountain"),
    synopsis = Synopsis.unsafe("Synopsis"),
    writers = List(Persons.person1.id, Persons.person2.id),
    cast = List(
      CastMember.unsafe(
        actor = Persons.person3.id,
        roles = List(ActorRole.unsafe("Hero"), ActorRole.unsafe("Narator")),
        main = true,
      ),
      CastMember.unsafe(
        actor = Persons.person2.id,
        roles = List(ActorRole.unsafe("Villian")),
        main = false,
      ),
    ),
    releaseDate = makeReleaseDate(2000, 10, 10),
    series = makeSeries("1e0a7f74-8143-4477-ae0f-33547de9c53f", "Series"),
    seriesSeason = AudioPlaySeason.unsafe(1).some,
    seriesNumber = AudioPlaySeriesNumber.unsafe(1).some,
    coverUrl = makeCoverUri("https://imagahost.org/123"),
    externalResources = List(
      ExternalResource(Purchase, URI.create("https://test.org/1")),
      ExternalResource(Download, URI.create("https://test.org/2")),
      ExternalResource(Streaming, URI.create("https://test.org/1")),
      ExternalResource(Other, URI.create("https://test.org/2")),
      ExternalResource(Private, URI.create("https://test.org/3")),
    ),
  )

  /** ''Test of Thing'' audio play. */
  val audioPlay2: AudioPlay = AudioPlay.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea67"),
    title = AudioPlayTitle.unsafe("Test of Thing"),
    synopsis = Synopsis.unsafe("Synopsis 1"),
    releaseDate = makeReleaseDate(1999, 10, 3),
    writers = Nil,
    cast = Nil,
    series = makeSeries("e810039b-c44c-405f-a360-e44fadc43ead", "Series"),
    seriesSeason = None,
    seriesNumber = AudioPlaySeriesNumber.unsafe(2).some,
    coverUrl = makeCoverUri("https://cdn.test.org/23"),
    externalResources =
      List(ExternalResource(Download, URI.create("https://audio.com/1"))),
  )

  /** ''The Testing Things'' audio play. */
  val audioPlay3: AudioPlay = AudioPlay.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea68"),
    title = AudioPlayTitle.unsafe("The Testing Things"),
    synopsis = Synopsis.unsafe("Synopsis 2"),
    releaseDate = makeReleaseDate(2024, 3, 15),
    writers = Nil,
    cast = List(
      CastMember.unsafe(
        actor = Persons.person3.id,
        roles = List(ActorRole.unsafe("Whatever")),
        main = false,
      ),
    ),
    series = None,
    seriesSeason = None,
    seriesNumber = None,
    coverUrl = None,
    externalResources =
      List(ExternalResource(Streaming, URI.create("https://audio.com/2"))),
  )

  /** Stub [[AudioPlayService]] implementation that supports only `findById` and
   *  `search` operations.
   *
   *  Contains only persons given in [[AudioPlays]] object.
   *
   *  @tparam F effect type.
   */
  def service[F[_]: Applicative]: AudioPlayService[F] = new AudioPlayService[F]:
    private val audioById: Map[Uuid[AudioPlay], AudioPlay] = Map.from(
      List(audioPlay1, audioPlay2, audioPlay3).map(p => (p.id, p)),
    )

    override def get(
        request: GetAudioPlayRequest,
    ): F[Either[ErrorResponse, AudioPlayResource]] = audioById
      .get(Uuid[AudioPlay](request.name))
      .map(AudioPlayMapper.toResponse(_, Persons.resourceById))
      .toRight(AudioPlayServiceErrorResponses.audioPlayNotFound)
      .pure[F]

    override def list(
        request: ListAudioPlaysRequest,
    ): F[Either[ErrorResponse, ListAudioPlaysResponse]] =
      throw new UnsupportedOperationException()

    override def search(
        request: SearchAudioPlaysRequest,
    ): F[Either[ErrorResponse, SearchAudioPlaysResponse]] =
      val elements = audioById.values
        .filter(a => a.title == request.query)
        .toList
      AudioPlayMapper
        .toSearchResponse(elements, Persons.resourceById)
        .asRight
        .pure[F]

    override def create(
        user: User,
        request: CreateAudioPlayRequest,
    ): F[Either[ErrorResponse, AudioPlayResource]] =
      throw new UnsupportedOperationException()

    override def delete(
        user: User,
        request: DeleteAudioPlayRequest,
    ): F[Either[ErrorResponse, Unit]] =
      throw new UnsupportedOperationException()
