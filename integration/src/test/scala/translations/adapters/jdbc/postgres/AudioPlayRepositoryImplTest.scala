package org.aulune
package translations.adapters.jdbc.postgres


import shared.adapters.repositories.jdbc.postgres.PostgresTestContainer
import shared.errors.RepositoryError
import shared.errors.RepositoryError.*
import translations.application.repositories.AudioPlayRepository
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.domain.model.audioplay.AudioPlay
import translations.domain.shared.ExternalResource
import translations.domain.shared.ExternalResourceType.*

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI
import java.util.UUID


final class AudioPlayRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:
  private def stand = makeStand(AudioPlayRepositoryImpl.build[IO])

  private val seriesId =
    Some(UUID.fromString("ec80c1fb-6e5d-4654-b258-55f1e91c150a"))
  private val resources = List(
    ExternalResource(Purchase, URI.create("https://test.org/1").toURL),
    ExternalResource(Download, URI.create("https://test.org/2").toURL),
    ExternalResource(Streaming, URI.create("https://test.org/1").toURL),
    ExternalResource(Other, URI.create("https://test.org/2").toURL),
    ExternalResource(Private, URI.create("https://test.org/3").toURL),
  )

  /* All AudioPlay are constructed using unsafe Option `get` method.
  It's intentional to always keep tests up-to-date with changes in AudioPlay. */
  private val audioPlayTest: AudioPlay = AudioPlay(
    id = UUID.fromString("3f8a202e-609d-49b2-a643-907b341cea66"),
    title = "Audio Play Title",
    seriesId = seriesId,
    seriesNumber = Some(1),
    coverUrl = None,
    externalResources = resources,
  ).toOption.get

  private val updatedAudioPlayTest: AudioPlay = AudioPlay
    .update(
      audioPlayTest,
      title = "Updated",
      seriesId = seriesId,
      seriesNumber = Some(2),
      coverUrl = Some(URI.create("https://cdn.test.org/1").toURL),
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

  "persist method " - {
    "should " - {
      "retrieve audio plays" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest)
          audio <- repo.get(audioPlayTest.id)
        yield audio shouldBe Some(audioPlayTest)
      }

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
    AudioPlay(
      id = UUID.fromString("e87d47de-50c9-4588-aba9-b0c30637f7de"),
      title = "Audio Play 1",
      seriesId = Some(UUID.fromString("e810039b-c44c-405f-a360-e44fadc43ead")),
      seriesNumber = Some(2),
      coverUrl = Some(URI.create("https://cdn.test.org/23").toURL),
      externalResources = List(
        ExternalResource(Download, URI.create("https://audio.com/1").toURL)),
    ).toOption.get,
    AudioPlay(
      id = UUID.fromString("978f8a9e-800a-4f1f-84ac-819a61916f46"),
      title = "Audio Play 2",
      seriesId = None,
      seriesNumber = None,
      coverUrl = None,
      externalResources = List(
        ExternalResource(Streaming, URI.create("https://audio.com/2").toURL)),
    ).toOption.get,
    AudioPlay(
      id = UUID.fromString("cca461a3-5f4b-49ec-807e-4d29e8ae5f44"),
      title = "Audio Play 3",
      seriesId = None,
      seriesNumber = None,
      coverUrl = Some(URI.create("https://cdn.test.org/53").toURL),
      externalResources = List(
        ExternalResource(Streaming, URI.create("https://audio.com/3").toURL)),
    ).toOption.get,
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
          token = AudioPlayToken(first.id)
          rest <- repo.list(Some(token), 1)
        yield rest.head shouldBe audioPlayTests(1)
      }
    }
  }
end AudioPlayRepositoryImplTest
