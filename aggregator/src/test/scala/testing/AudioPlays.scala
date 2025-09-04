package org.aulune.aggregator
package testing


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
import domain.shared.{ExternalResource, ImageUrl, ReleaseDate, Synopsis}

import cats.syntax.all.given
import org.aulune.commons.types.Uuid

import java.net.URI
import java.time.LocalDate


/** [[AudioPlay]] objects to use in tests. */
private[aggregator] object AudioPlays:
  private def makeCoverUrl(url: String): Option[ImageUrl] =
    ImageUrl.unsafe(URI.create(url).toURL).some

  private def makeReleaseDate(year: Int, month: Int, day: Int): ReleaseDate =
    ReleaseDate.unsafe(LocalDate.of(year, month, day))

  private def makeSeries(uuid: String, name: String): Option[AudioPlaySeries] =
    AudioPlaySeries
      .unsafe(Uuid.unsafe(uuid), AudioPlaySeriesName.unsafe(name))
      .some

  val audioPlay1: AudioPlay = AudioPlay.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea66"),
    title = AudioPlayTitle.unsafe("Title"),
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
    coverUrl = makeCoverUrl("https://imagahost.org/123"),
    externalResources = List(
      ExternalResource(Purchase, URI.create("https://test.org/1").toURL),
      ExternalResource(Download, URI.create("https://test.org/2").toURL),
      ExternalResource(Streaming, URI.create("https://test.org/1").toURL),
      ExternalResource(Other, URI.create("https://test.org/2").toURL),
      ExternalResource(Private, URI.create("https://test.org/3").toURL),
    ),
  )

  val audioPlay2: AudioPlay = AudioPlay.unsafe(
    id = Uuid.unsafe("0198d217-2e95-7b94-80a7-a762589de506"),
    title = AudioPlayTitle.unsafe("Audio Play 1"),
    synopsis = Synopsis.unsafe("Synopsis 1"),
    releaseDate = makeReleaseDate(1999, 10, 3),
    writers = Nil,
    cast = Nil,
    series = makeSeries("e810039b-c44c-405f-a360-e44fadc43ead", "Series"),
    seriesSeason = None,
    seriesNumber = AudioPlaySeriesNumber.unsafe(2).some,
    coverUrl = makeCoverUrl("https://cdn.test.org/23"),
    externalResources =
      List(ExternalResource(Download, URI.create("https://audio.com/1").toURL)),
  )

  val audioPlay3: AudioPlay = AudioPlay.unsafe(
    id = Uuid.unsafe("0198d217-859b-71b7-947c-dd2548d7f8f4"),
    title = AudioPlayTitle.unsafe("Audio Play 2"),
    synopsis = Synopsis.unsafe("Synopsis 2"),
    releaseDate = makeReleaseDate(2024, 3, 15),
    writers = Nil,
    cast = List(
      CastMember.unsafe(
        actor = Uuid.unsafe("2eb87946-4c6c-40a8-ae80-a05f0df355f8"),
        roles = List(ActorRole.unsafe("Whatever")),
        main = false,
      ),
    ),
    series = None,
    seriesSeason = None,
    seriesNumber = None,
    coverUrl = None,
    externalResources =
      List(ExternalResource(Streaming, URI.create("https://audio.com/2").toURL)),
  )
