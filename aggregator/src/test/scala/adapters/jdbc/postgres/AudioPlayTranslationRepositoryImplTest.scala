package org.aulune.aggregator
package adapters.jdbc.postgres


import application.repositories.AudioPlayTranslationRepository
import application.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor
import domain.model.audioplay.{AudioPlay, AudioPlayTranslation}
import domain.shared.TranslatedTitle
import testing.AudioPlayTranslations

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.aulune.commons.repositories.RepositoryError.{
  AlreadyExists,
  FailedPrecondition,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.types.Uuid
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.net.URI


/** Tests for [[AudioPlayTranslationRepositoryImpl]]. */
final class AudioPlayTranslationRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(AudioPlayTranslationRepositoryImpl.build[IO])

  private val translationTest = AudioPlayTranslations.translation1
  private val translationTests = List(
    AudioPlayTranslations.translation1,
    AudioPlayTranslations.translation2,
    AudioPlayTranslations.translation3)
  private val updatedTranslationTest = translationTest
    .update(
      title = TranslatedTitle.unsafe("Updated"),
      links = NonEmptyList(URI.create("https://testing.org"), Nil),
    )
    .getOrElse(throw new IllegalStateException())

  "contains method " - {
    "should " - {
      "return false for non-existent translations" in stand { repo =>
        for exists <- repo.contains(translationTest.id)
        yield exists shouldBe false
      }

      "return true for existent translation" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          exists <- repo.contains(translationTest.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "retrieve translations" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          audio <- repo.get(translationTest.id)
        yield audio shouldBe Some(translationTest)
      }

      "return `None` for non-existent translation" in stand { repo =>
        for audio <- repo.get(translationTest.id)
        yield audio shouldBe None
      }
    }
  }

  "persist method " - {
    "should " - {
      "throw error if an translation exists" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          result <- repo.persist(updatedTranslationTest).attempt
        yield result shouldBe Left(AlreadyExists)
      }
    }
  }

  "update method" - {
    "should " - {
      "update translations" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          updated <- repo.update(updatedTranslationTest)
        yield updated shouldBe updatedTranslationTest
      }

      "throw error for non-existent translations" in stand { repo =>
        for updated <- repo.update(translationTest).attempt
        yield updated shouldBe Left(FailedPrecondition)
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          updated <- repo.update(updatedTranslationTest)
          updated <- repo.update(updatedTranslationTest)
        yield updated shouldBe updatedTranslationTest
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete translations" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          result <- repo.delete(translationTest.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(translationTest)
          _ <- repo.delete(translationTest.id)
          result <- repo.delete(translationTest.id)
        yield result shouldBe ()
      }
    }
  }

  "list method " - {
    "should " - {
      "return empty list if no translation's available" in stand { repo =>
        for audios <- repo.list(None, 10)
        yield audios shouldBe Nil
      }

      "return no more than asked" in stand { repo =>
        for
          _ <- persistTranslations(repo)
          audios <- repo.list(None, 2)
        yield audios shouldBe translationTests.take(2)
      }

      "continue listing if token is given" in stand { repo =>
        for
          _ <- persistTranslations(repo)
          first <- repo.list(None, 1).map(_.head)
          cursor = AudioPlayTranslationCursor(first.id)
          rest <- repo.list(Some(cursor), 1)
        yield rest.head shouldBe translationTests(1)
      }
    }
  }

  private def persistTranslations(repo: AudioPlayTranslationRepository[IO]) =
    translationTests.foldLeft(IO.unit) { (io, audio) =>
      io >> repo.persist(audio).void
    }

end AudioPlayTranslationRepositoryImplTest
