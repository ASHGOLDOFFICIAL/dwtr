package org.aulune.aggregator
package adapters.jdbc.postgres


import adapters.service.Persons
import domain.model.person.{FullName, Person}
import domain.repositories.PersonRepository

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.repositories.RepositoryError.{
  AlreadyExists,
  FailedPrecondition,
  InvalidArgument,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[PersonRepositoryImpl]]. */
final class PersonRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(PersonRepositoryImpl.build[IO])

  private val person = Persons.person1
  private val updatedPerson = person
    .update(
      name = FullName.unsafe("John Brown"),
    )
    .getOrElse(throw new IllegalStateException())
  private val personList = List(
    Persons.person1,
    Persons.person2,
    Persons.person3,
  )

  "contains method " - {
    "should " - {
      "return false for non-existent person" in stand { repo =>
        for exists <- repo.contains(person.id)
        yield exists shouldBe false
      }

      "return true for existent person" in stand { repo =>
        for
          _ <- repo.persist(person)
          exists <- repo.contains(person.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "return `None` for non-existent person" in stand { repo =>
        for audio <- repo.get(person.id)
        yield audio shouldBe None
      }

      "retrieve existing persons" in stand { repo =>
        for
          _ <- repo.persist(person)
          audio <- repo.get(person.id)
        yield audio shouldBe Some(person)
      }
    }
  }

  "persist method " - {
    "should " - {
      "throw error if an person exists" in stand { repo =>
        for
          _ <- repo.persist(person)
          result <- repo.persist(updatedPerson).attempt
        yield result shouldBe Left(AlreadyExists)
      }
    }
  }

  "update method" - {
    "should " - {
      "update persons" in stand { repo =>
        for
          _ <- repo.persist(person)
          updated <- repo.update(updatedPerson)
        yield updated shouldBe updatedPerson
      }

      "throw error for non-existent person" in stand { repo =>
        for updated <- repo.update(person).attempt
        yield updated shouldBe Left(FailedPrecondition)
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(person)
          updated <- repo.update(updatedPerson)
          updated <- repo.update(updatedPerson)
        yield updated shouldBe updatedPerson
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete persons" in stand { repo =>
        for
          _ <- repo.persist(person)
          result <- repo.delete(person.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(person)
          _ <- repo.delete(person.id)
          result <- repo.delete(person.id)
        yield result shouldBe ()
      }
    }
  }

  "batchGet method " - {
    "should " - {
      "get elements in batches" in stand { repo =>
        val ids = NonEmptyList.of(Persons.person1.id, Persons.person2.id)
        for
          _ <- persistMany(repo)
          result <- repo.batchGet(ids)
        yield result shouldBe List(Persons.person1, Persons.person2)
      }

      "skip missing elements" in stand { repo =>
        val missingId =
          Uuid.unsafe[Person]("1dbcb7ed-8c13-40c6-b4be-d4b323535d2b")
        val ids = NonEmptyList.of(Persons.person1.id, missingId)
        for
          _ <- persistMany(repo)
          result <- repo.batchGet(ids)
        yield result shouldBe List(Persons.person1)
      }

      "return empty list when none is found" in stand { repo =>
        val ids = NonEmptyList.of(Persons.person1.id)
        for result <- repo.batchGet(ids)
        yield result shouldBe Nil
      }
    }
  }

  "list method " - {
    "should " - {
      "return empty list if no person's available" in stand { repo =>
        for audios <- repo.list(None, 10)
        yield audios shouldBe Nil
      }

      "return no more than asked" in stand { repo =>
        for
          _ <- persistMany(repo)
          audios <- repo.list(None, 2)
        yield audios shouldBe personList.take(2)
      }

      "continue listing if token is given" in stand { repo =>
        for
          _ <- persistMany(repo)
          first <- repo.list(None, 1).map(_.head)
          cursor = PersonRepository.Cursor(first.id)
          rest <- repo.list(Some(cursor), 1)
        yield rest.head shouldBe personList(1)
      }
    }
  }

  "search method " - {
    "should " - {
      "return matching elements" in stand { repo =>
        val element = Persons.person1
        val query = NonEmptyString.unsafe(element.name)
        for
          _ <- persistMany(repo)
          result <- repo.search(query, 3)
        yield result should contain(element)
      }

      "not return elements when none of them match" in stand { repo =>
        for
          _ <- persistMany(repo)
          result <- repo.search(NonEmptyString.unsafe("nothing"), 1)
        yield result shouldBe Nil
      }

      "return elements in order of likeness" in stand { repo =>
        for
          _ <- persistMany(repo)
          result <- repo.search(NonEmptyString.unsafe("smith"), 3)
        yield result shouldBe List(Persons.person1, Persons.person2)
      }

      "throw InvalidArgument when given non-positive limit" in stand { repo =>
        for result <- repo.search(NonEmptyString.unsafe("something"), 0).attempt
        yield result shouldBe InvalidArgument.asLeft
      }
    }
  }

  private def persistMany(repo: PersonRepository[IO]) =
    personList.foldLeft(IO.unit)((io, audio) => io >> repo.persist(audio).void)

end PersonRepositoryImplTest
