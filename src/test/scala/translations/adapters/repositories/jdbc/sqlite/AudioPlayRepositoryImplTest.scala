package org.aulune
package translations.adapters.repositories.jdbc.sqlite


import shared.adapters.repositories.jdbc.sqlite.SqliteStand
import shared.errors.RepositoryError
import shared.errors.RepositoryError.AlreadyExists
import translations.adapters.jdbc.sqlite.AudioPlayRepositoryImpl
import translations.domain.model.audioplay.AudioPlay

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant
import java.util.UUID


final class AudioPlayRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers:
  private def stand = SqliteStand(AudioPlayRepositoryImpl.build[IO])

  val audioPlayTest1: AudioPlay = AudioPlay(
    id = UUID.fromString("3f8a202e-609d-49b2-a643-907b341cea66"),
    title = "Audio Play Title",
    seriesId = Some(UUID.fromString("ec80c1fb-6e5d-4654-b258-55f1e91c150a")),
    seriesNumber = Some(1),
    addedAt = Instant.parse("2000-01-01T12:00:00Z"),
  ).toOption.get // If it's invalid, it MUST throw.

  val audioPlayTest2: AudioPlay = AudioPlay(
    id = UUID.fromString("8108db39-2dd1-4728-9f12-1c11c9581b5e"),
    title = "Audio Play Title 2: The Return of UUIDs",
    seriesId = Some(UUID.fromString("60116321-c623-4ea6-855a-a3409c4734d0")),
    seriesNumber = Some(2),
    addedAt = Instant.parse("2001-01-01T12:00:00Z"),
  ).toOption.get // If it's invalid, it MUST throw.

  "AudioPlayRepository implementation via sqlite " - {
    "should " - {
      "persist and retrieve audio plays" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest1)
          user <- repo.get(audioPlayTest1.id)
        yield user shouldBe Some(audioPlayTest1)
      }

      "return `None` when getting non-existent audio play" in stand { repo =>
        for user <- repo.get(audioPlayTest2.id)
        yield user shouldBe None
      }

      "not persist if an audio play exists" in stand { repo =>
        for
          _ <- repo.persist(audioPlayTest1)
          result <- repo.persist(audioPlayTest1)
        yield result match
          case Left(err) => println(err.toString); err shouldBe AlreadyExists
          case Right(_)  => fail("Error was expected.")
      }
    }
  }
