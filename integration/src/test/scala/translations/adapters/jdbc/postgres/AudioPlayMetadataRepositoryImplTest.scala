package org.aulune
package translations.adapters.jdbc.postgres


import shared.adapters.repositories.jdbc.postgres.PostgresTestContainer
import shared.errors.RepositoryError
import shared.errors.RepositoryError.*
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.application.repositories.{
  AudioPlayMetadata,
  AudioPlayMetadataRepository,
  AudioPlayRepository,
}
import translations.domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesName,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
}
import translations.domain.shared.ExternalResourceType.*
import translations.domain.shared.{
  ExternalResource,
  ImageUrl,
  ReleaseDate,
  Synopsis,
  Uuid,
}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import java.time.LocalDate
import java.util.UUID


/** Tests for [[AudioPlayMetadataRepositoryImpl]]. */
final class AudioPlayMetadataRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:
  private def stand = makeStand(AudioPlayMetadataRepositoryImpl.build[IO])

  /* All value objects are constructed using unsafe Option `get` method.
  It's intentional to always keep tests up-to-date with changes in model. */
  private def makeUuid[A](uuid: String): Uuid[A] =
    Uuid[A](UUID.fromString(uuid))
  private def makeTitle(str: String): AudioPlayTitle = AudioPlayTitle(str).get
  private def makeSynopsis(str: String): Synopsis = Synopsis(str).get
  private def makeReleaseDate(year: Int, month: Int, day: Int): ReleaseDate =
    ReleaseDate(LocalDate.of(year, month, day)).get
  private def makeSeries(uuid: String, name: String): Option[AudioPlaySeries] =
    Some(
      AudioPlaySeries(
        id = makeUuid(uuid),
        name = AudioPlaySeriesName(name).get,
      ).get)
  private def makeSeason(int: Int): Option[AudioPlaySeason] =
    Some(AudioPlaySeason(int).get)
  private def makeSeriesNumber(int: Int): Option[AudioPlaySeriesNumber] =
    Some(AudioPlaySeriesNumber(int).get)
  private def makeCoverUrl(url: String): Option[ImageUrl] =
    Some(ImageUrl(URI.create(url).toURL).get)

  private val resources = List(
    ExternalResource(Purchase, URI.create("https://test.org/1").toURL),
    ExternalResource(Download, URI.create("https://test.org/2").toURL),
    ExternalResource(Streaming, URI.create("https://test.org/1").toURL),
    ExternalResource(Other, URI.create("https://test.org/2").toURL),
    ExternalResource(Private, URI.create("https://test.org/3").toURL),
  )

  private val audioPlayTest = AudioPlayMetadata(
    id = makeUuid("3f8a202e-609d-49b2-a643-907b341cea66"),
    title = makeTitle("Title"),
    synopsis = makeSynopsis("Synopsis"),
    releaseDate = makeReleaseDate(2000, 10, 10),
    series = makeSeries("1e0a7f74-8143-4477-ae0f-33547de9c53f", "Series"),
    seriesSeason = makeSeason(1),
    seriesNumber = makeSeriesNumber(1),
    coverUrl = None,
    externalResources = resources,
  )

  private val updatedAudioPlayTest = audioPlayTest.copy(
    title = makeTitle("Updated"),
    coverUrl = makeCoverUrl("https://cdn.test.org/1"),
    externalResources =
      List(ExternalResource(Purchase, URI.create("https://test.org/1").toURL)),
  )

  "contains method " - {
    "should " - {
      "return false for non-existent audio play" in stand { repo =>
        for exists <- repo.contains(audioPlayTest.id)
        yield exists shouldBe false
      }

      "return true for existent audio play" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          exists <- repo.contains(audioPlayTest.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "retrieve audio plays with resources" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          audio <- repo.get(audioPlayTest.id)
        yield audio shouldBe Some(audioPlayTest)
      }

      "retrieve audio plays without resources" in stand { repo =>
        val without = audioPlayTest.copy(externalResources = Nil)
        for
          _ <- repo.persist(without)
          audio <- repo.get(without.id)
        yield audio shouldBe Some(without)
      }
    }
  }

  "persist method " - {
    "should " - {
      "return `None` for non-existent audio play" in stand { repo =>
        for audio <- repo.get(audioPlayTest.id)
        yield audio shouldBe None
      }

      "throw error if an audio play exists" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          result <- repo.persist(updatedAudioPlayTest).attempt
        yield result shouldBe Left(AlreadyExists)
      }
    }
  }

  "update method" - {
    "should " - {
      "update audio plays" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          updated <- repo.update(updatedAudioPlayTest)
        yield updated shouldBe updatedAudioPlayTest
      }

      "throw error for non-existent audio plays" in stand { repo =>
        for updated <- repo.update(audioPlayTest).attempt
        yield updated shouldBe Left(NothingToUpdate)
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          updated <- repo.update(updatedAudioPlayTest)
          updated <- repo.update(updatedAudioPlayTest)
        yield updated shouldBe updatedAudioPlayTest
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete audio plays" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          result <- repo.delete(audioPlayTest.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          _ <- repo.delete(audioPlayTest.id)
          result <- repo.delete(audioPlayTest.id)
        yield result shouldBe ()
      }
    }
  }

  private val audioPlayTests = List(
    AudioPlayMetadata(
      id = makeUuid("0198d217-2e95-7b94-80a7-a762589de506"),
      title = makeTitle("Audio Play 1"),
      synopsis = makeSynopsis("Synopsis 1"),
      releaseDate = makeReleaseDate(1999, 10, 3),
      series = makeSeries("e810039b-c44c-405f-a360-e44fadc43ead", "Series"),
      seriesSeason = None,
      seriesNumber = makeSeriesNumber(2),
      coverUrl = makeCoverUrl("https://cdn.test.org/23"),
      externalResources = List(
        ExternalResource(Download, URI.create("https://audio.com/1").toURL)),
    ),
    AudioPlayMetadata(
      id = makeUuid("0198d217-859b-71b7-947c-dd2548d7f8f4"),
      title = makeTitle("Audio Play 2"),
      synopsis = makeSynopsis("Synopsis 2"),
      releaseDate = makeReleaseDate(2024, 3, 15),
      series = None,
      seriesSeason = None,
      seriesNumber = None,
      coverUrl = None,
      externalResources = List(
        ExternalResource(Streaming, URI.create("https://audio.com/2").toURL)),
    ),
    AudioPlayMetadata(
      id = makeUuid("0198d217-b41c-7e8d-95c8-ed78fd168d0d"),
      title = makeTitle("Audio Play 3"),
      synopsis = makeSynopsis("Synopsis 3"),
      releaseDate = makeReleaseDate(2007, 7, 8),
      series = None,
      seriesSeason = None,
      seriesNumber = None,
      coverUrl = makeCoverUrl("https://cdn.test.org/53"),
      externalResources = List(
        ExternalResource(Streaming, URI.create("https://audio.com/3").toURL)),
    ),
  )
  private def persistAudios(repo: AudioPlayMetadataRepository[IO]) =
    audioPlayTests.foldLeft(IO.unit) { (io, audio) =>
      io >> repo.persist(audio).void
    }

  "list method " - {
    "should " - {
      "return empty list if no audio play's available" in stand { repo =>
        for audios <- repo.list(None, 10)
        yield audios shouldBe Nil
      }

      "return no more than asked" in stand { repo =>
        for
          _ <- persistAudios(repo)
          audios <- repo.list(None, 2)
        yield audios shouldBe audioPlayTests.take(2)
      }

      "continue listing if token is given" in stand { repo =>
        for
          _ <- persistAudios(repo)
          first <- repo.list(None, 1).map(_.head)
          token = AudioPlayToken(first.id)
          rest <- repo.list(Some(token), 1)
        yield rest.head shouldBe audioPlayTests(1)
      }
    }
  }

  "getSeries method " - {
    "should " - {
      "return None if series doesn't exist" in stand { repo =>
        val nonExistent =
          makeUuid[AudioPlaySeries]("7efa6d44-ffd6-4668-94a2-7650821cd9f9")
        for result <- repo.getSeries(nonExistent)
        yield result shouldBe None
      }

      "return correct series if it exists" in stand { repo =>
        val series =
          makeSeries("1bf2deb3-2915-4673-a7da-976343776f1d", "New Series")
        for
          _ <- repo.persist(audioPlayTest.copy(series = series))
          result <- repo.getSeries(series.get.id)
        yield result shouldBe series
      }
    }
  }
end AudioPlayMetadataRepositoryImplTest
