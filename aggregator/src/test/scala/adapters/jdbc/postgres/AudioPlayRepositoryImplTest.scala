package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.service.AudioPlays
import domain.model.audioplay.{
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import domain.repositories.AudioPlayRepository
import domain.repositories.AudioPlayRepository.AudioPlayCursor
import org.aulune.aggregator.domain.model.shared.ExternalResourceType.Purchase

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.aggregator.domain.model.audioplay.series.{
  AudioPlaySeries,
  AudioPlaySeriesName,
}
import org.aulune.aggregator.domain.model.shared.{
  ExternalResource,
  ImageUri,
  ReleaseDate,
  Synopsis,
}
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  AlreadyExists,
  FailedPrecondition,
  InvalidArgument,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI


/** Tests for [[AudioPlayRepositoryImpl]]. */
final class AudioPlayRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(AudioPlayRepositoryImpl.build[IO])

  private val audioPlayTest = AudioPlays.audioPlay1
  private val audioPlayTests =
    List(AudioPlays.audioPlay1, AudioPlays.audioPlay2, AudioPlays.audioPlay3)
  private val updatedAudioPlayTest = audioPlayTest
    .update(
      title = AudioPlayTitle.unsafe("Updated"),
      writers = List(Uuid.unsafe("c5c1f3b9-175c-4fa2-800d-c9c20cb44539")),
      externalResources =
        List(ExternalResource(Purchase, URI.create("https://test.org/1"))),
    )
    .getOrElse(throw new IllegalStateException())

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

  "search method " - {
    "should " - {
      "return matching elements" in stand { repo =>
        val element = AudioPlays.audioPlay2
        val query = NonEmptyString.unsafe(element.title)
        for
          _ <- persistAudios(repo)
          result <- repo.search(query, 3)
        yield result should contain(element)
      }

      "not return elements when none of them match" in stand { repo =>
        for
          _ <- persistAudios(repo)
          result <- repo.search(NonEmptyString.unsafe("nothing"), 1)
        yield result shouldBe Nil
      }

      "return elements in order of likeness" in stand { repo =>
        for
          _ <- persistAudios(repo)
          result <- repo.search(NonEmptyString.unsafe("test thing"), 3)
        yield result shouldBe List(AudioPlays.audioPlay3, AudioPlays.audioPlay2)
      }

      "throw InvalidArgument when given non-positive limit" in stand { repo =>
        for result <- repo.search(NonEmptyString.unsafe("something"), 0).attempt
        yield result shouldBe InvalidArgument.asLeft
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
        val series = AudioPlaySeries
          .unsafe(
            Uuid.unsafe("1bf2deb3-2915-4673-a7da-976343776f1d"),
            AudioPlaySeriesName.unsafe("New Series"))
          .some
        for
          _ <- repo.persist(audioPlayTest.update(series = series).toOption.get)
          result <- repo.getSeries(series.get.id)
        yield result shouldBe series
      }
    }
  }

  private def persistAudios(repo: AudioPlayRepository[IO]) =
    audioPlayTests.foldLeft(IO.unit) { (io, audio) =>
      io >> repo.persist(audio).void
    }

end AudioPlayRepositoryImplTest
