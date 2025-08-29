package org.aulune
package aggregator.adapters.jdbc.postgres


import commons.types.Uuid
import commons.repositories.RepositoryError.{AlreadyExists, FailedPrecondition}
import commons.testing.PostgresTestContainer
import aggregator.application.repositories.AudioPlayRepository
import aggregator.application.repositories.AudioPlayRepository.AudioPlayCursor
import aggregator.domain.model.audioplay.{
  ActorRole,
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeries,
  AudioPlaySeriesName,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import aggregator.domain.shared.ExternalResourceType.*
import aggregator.domain.shared.{
  ExternalResource,
  ImageUrl,
  ReleaseDate,
  Synopsis,
}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import java.time.LocalDate


/** Tests for [[AudioPlayRepositoryImpl]]. */
final class AudioPlayRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:
  private def stand = makeStand(AudioPlayRepositoryImpl.build[IO])

  private def makeReleaseDate(year: Int, month: Int, day: Int): ReleaseDate =
    ReleaseDate.unsafe(LocalDate.of(year, month, day))
  private def makeSeries(uuid: String, name: String): Option[AudioPlaySeries] =
    Some(
      AudioPlaySeries.unsafe(
        id = Uuid.unsafe(uuid),
        name = AudioPlaySeriesName.unsafe(name),
      ))
  private def makeSeason(int: Int): Option[AudioPlaySeason] =
    Some(AudioPlaySeason.unsafe(int))
  private def makeSeriesNumber(int: Int): Option[AudioPlaySeriesNumber] =
    Some(AudioPlaySeriesNumber.unsafe(int))
  private def makeCoverUrl(url: String): Option[ImageUrl] =
    Some(ImageUrl.unsafe(URI.create(url).toURL))

  private val resources = List(
    ExternalResource(Purchase, URI.create("https://test.org/1").toURL),
    ExternalResource(Download, URI.create("https://test.org/2").toURL),
    ExternalResource(Streaming, URI.create("https://test.org/1").toURL),
    ExternalResource(Other, URI.create("https://test.org/2").toURL),
    ExternalResource(Private, URI.create("https://test.org/3").toURL),
  )

  private val audioPlayTest = AudioPlay.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea66"),
    title = AudioPlayTitle.unsafe("Title"),
    synopsis = Synopsis.unsafe("Synopsis"),
    writers = List(
      Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950481"),
      Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950482"),
      Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950483"),
      Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950484"),
      Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950485"),
      Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950486"),
      Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950487"),
    ),
    cast = List(
      CastMember.unsafe(
        actor = Uuid.unsafe("adfeccac-0c8e-4a6c-a0b3-08684e6bd336"),
        roles = List(ActorRole.unsafe("Hero"), ActorRole.unsafe("Narator")),
        main = true,
      ),
      CastMember.unsafe(
        actor = Uuid.unsafe("64729185-54b2-46f6-97d3-97678a4802a7"),
        roles = List(ActorRole.unsafe("Villian")),
        main = true,
      ),
      CastMember.unsafe(
        actor = Uuid.unsafe("e0ee5ca8-d5ca-4b1a-a0e6-719a9ed3b18f"),
        roles = List(ActorRole.unsafe("Some guy")),
        main = false,
      ),
    ),
    releaseDate = makeReleaseDate(2000, 10, 10),
    series = makeSeries("1e0a7f74-8143-4477-ae0f-33547de9c53f", "Series"),
    seriesSeason = makeSeason(1),
    seriesNumber = makeSeriesNumber(1),
    coverUrl = None,
    externalResources = resources,
  )

  private val updatedAudioPlayTest = audioPlayTest
    .update(
      title = AudioPlayTitle.unsafe("Updated"),
      writers = List(Uuid.unsafe("c5c1f3b9-175c-4fa2-800d-c9c20cb44539")),
      coverUrl = makeCoverUrl("https://cdn.test.org/1"),
      externalResources =
        List(ExternalResource(Purchase, URI.create("https://test.org/1").toURL)),
    )
    .toOption
    .get

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
      "retrieve audio plays with writers and resources" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          audio <- repo.get(audioPlayTest.id)
        yield audio shouldBe Some(audioPlayTest)
      }

      "retrieve audio plays without resources" in stand { repo =>
        val without = audioPlayTest
          .update(externalResources = Nil)
          .toOption
          .get
        for
          _ <- repo.persist(without)
          audio <- repo.get(without.id)
        yield audio shouldBe Some(without)
      }

      "retrieve audio plays without writers" in stand { repo =>
        val without = audioPlayTest.update(writers = Nil).toOption.get
        for
          _ <- repo.persist(without)
          audio <- repo.get(without.id)
        yield audio shouldBe Some(without)
      }

      "return `None` for non-existent audio play" in stand { repo =>
        for audio <- repo.get(audioPlayTest.id)
        yield audio shouldBe None
      }
    }
  }

  "persist method " - {
    "should " - {
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
        yield updated shouldBe Left(FailedPrecondition)
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
    AudioPlay.unsafe(
      id = Uuid.unsafe("0198d217-2e95-7b94-80a7-a762589de506"),
      title = AudioPlayTitle.unsafe("Audio Play 1"),
      synopsis = Synopsis.unsafe("Synopsis 1"),
      releaseDate = makeReleaseDate(1999, 10, 3),
      writers = Nil,
      cast = Nil,
      series = makeSeries("e810039b-c44c-405f-a360-e44fadc43ead", "Series"),
      seriesSeason = None,
      seriesNumber = makeSeriesNumber(2),
      coverUrl = makeCoverUrl("https://cdn.test.org/23"),
      externalResources = List(
        ExternalResource(Download, URI.create("https://audio.com/1").toURL)),
    ),
    AudioPlay.unsafe(
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
      externalResources = List(
        ExternalResource(Streaming, URI.create("https://audio.com/2").toURL)),
    ),
    AudioPlay.unsafe(
      id = Uuid.unsafe("0198d217-b41c-7e8d-95c8-ed78fd168d0d"),
      title = AudioPlayTitle.unsafe("Audio Play 3"),
      synopsis = Synopsis.unsafe("Synopsis 3"),
      releaseDate = makeReleaseDate(2007, 7, 8),
      cast = Nil,
      writers = List(Uuid.unsafe("8b78e607-7afe-434c-a426-63a4512f3bf5")),
      series = None,
      seriesSeason = None,
      seriesNumber = None,
      coverUrl = makeCoverUrl("https://cdn.test.org/53"),
      externalResources = List(
        ExternalResource(Streaming, URI.create("https://audio.com/3").toURL)),
    ),
  )
  private def persistAudios(repo: AudioPlayRepository[IO]) =
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
          cursor = AudioPlayCursor(first.id)
          rest <- repo.list(Some(cursor), 1)
        yield rest.head shouldBe audioPlayTests(1)
      }
    }
  }

  "getSeries method " - {
    "should " - {
      "return None if series doesn't exist" in stand { repo =>
        val nonExistent = Uuid
          .unsafe[AudioPlaySeries]("7efa6d44-ffd6-4668-94a2-7650821cd9f9")
        for result <- repo.getSeries(nonExistent)
        yield result shouldBe None
      }

      "return correct series if it exists" in stand { repo =>
        val series =
          makeSeries("1bf2deb3-2915-4673-a7da-976343776f1d", "New Series")
        for
          _ <- repo.persist(audioPlayTest.update(series = series).toOption.get)
          result <- repo.getSeries(series.get.id)
        yield result shouldBe series
      }
    }
  }
end AudioPlayRepositoryImplTest
