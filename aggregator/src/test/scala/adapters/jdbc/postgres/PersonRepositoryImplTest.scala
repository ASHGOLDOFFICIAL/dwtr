package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.service.Persons
import domain.model.person.{FullName, Person}
import domain.repositories.PersonRepository

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.aulune.commons.repositories.RepositoryError.{AlreadyExists, FailedPrecondition}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.types.Uuid
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[PersonRepositoryImpl]]. */
final class PersonRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(PersonRepositoryImpl.build[IO])

  private val personTest = Persons.person1
  private val updatedPersonTest = personTest
    .update(
      name = FullName.unsafe("John Brown"),
    )
    .getOrElse(throw new IllegalStateException())
  private val personTests = List(
    Persons.person1,
    Persons.person2,
    Persons.person3,
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
      "return `None` for non-existent person" in stand { repo =>
        for audio <- repo.get(personTest.id)
        yield audio shouldBe None
      }

      "retrieve existing persons" in stand { repo =>
        for
          _ <- repo.persist(personTest)
          audio <- repo.get(personTest.id)
        yield audio shouldBe Some(personTest)
      }
    }
  }

  "persist method " - {
    "should " - {
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
        yield updated shouldBe Left(FailedPrecondition)
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

  "batchGet method " - {
    "should " - {
      "get elements in batches" in stand { repo =>
        val ids = NonEmptyList.of(Persons.person1.id, Persons.person2.id)
        for
          _ <- persistPersons(repo, personTests)
          result <- repo.batchGet(ids)
        yield result shouldBe List(Persons.person1, Persons.person2)
      }

      "skip missing elements" in stand { repo =>
        val missingId = Uuid.unsafe[Person]("1dbcb7ed-8c13-40c6-b4be-d4b323535d2b")
        val ids = NonEmptyList.of(Persons.person1.id, missingId)
        for
          _ <- persistPersons(repo, personTests)
          result <- repo.batchGet(ids)
        yield result shouldBe List(Persons.person1)
      }
      
      "return empty list when none is found" in stand { repo =>
        val ids = NonEmptyList.of(Persons.person1.id)
        for
          result <- repo.batchGet(ids)
        yield result shouldBe Nil
      }
    }
  }

  private def persistPersons(
      repo: PersonRepository[IO],
      xs: List[Person],
  ): IO[Unit] = xs.foldLeft(IO.unit)((io, cur) => io >> repo.persist(cur).void)

end PersonRepositoryImplTest
