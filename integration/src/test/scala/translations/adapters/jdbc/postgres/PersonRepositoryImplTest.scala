package org.aulune
package translations.adapters.jdbc.postgres


import shared.adapters.repositories.jdbc.postgres.PostgresTestContainer
import shared.errors.RepositoryError.{AlreadyExists, NothingToUpdate}
import shared.model.Uuid
import translations.domain.model.person.{FullName, Person}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[PersonRepositoryImpl]]. */
final class PersonRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(PersonRepositoryImpl.build[IO])

  private val personTest = Person.unsafe(
    id = Uuid.unsafe("3f8a202e-609d-49b2-a643-907b341cea66"),
    name = FullName.unsafe("John Smith"),
  )

  private val updatedPersonTest = Person.unsafe(
    id = personTest.id,
    name = FullName.unsafe("John Brown"),
  )

  "contains method " - {
    "should " - {
      "return false for non-existent person" in stand { repo =>
        for exists <- repo.contains(personTest.id)
        yield exists shouldBe false
      }

      "return true for existent person" in stand { repo =>
        for
          _ <- repo.persist(personTest)
          exists <- repo.contains(personTest.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "retrieve persons with resources" in stand { repo =>
        for
          _ <- repo.persist(personTest)
          audio <- repo.get(personTest.id)
        yield audio shouldBe Some(personTest)
      }
    }
  }

  "persist method " - {
    "should " - {
      "return `None` for non-existent person" in stand { repo =>
        for audio <- repo.get(personTest.id)
        yield audio shouldBe None
      }

      "throw error if an person exists" in stand { repo =>
        for
          _ <- repo.persist(personTest)
          result <- repo.persist(updatedPersonTest).attempt
        yield result shouldBe Left(AlreadyExists)
      }
    }
  }

  "update method" - {
    "should " - {
      "update persons" in stand { repo =>
        for
          _ <- repo.persist(personTest)
          updated <- repo.update(updatedPersonTest)
        yield updated shouldBe updatedPersonTest
      }

      "throw error for non-existent person" in stand { repo =>
        for updated <- repo.update(personTest).attempt
        yield updated shouldBe Left(NothingToUpdate)
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(personTest)
          updated <- repo.update(updatedPersonTest)
          updated <- repo.update(updatedPersonTest)
        yield updated shouldBe updatedPersonTest
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete persons" in stand { repo =>
        for
          _ <- repo.persist(personTest)
          result <- repo.delete(personTest.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(personTest)
          _ <- repo.delete(personTest.id)
          result <- repo.delete(personTest.id)
        yield result shouldBe ()
      }
    }
  }

end PersonRepositoryImplTest
