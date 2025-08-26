package org.aulune
package translations.adapters.service


import auth.application.dto.AuthenticatedUser
import .Admin
import shared.errors.{ApplicationServiceError, RepositoryError}
import shared.model.Uuid
import translations.application.dto.person.{PersonRequest, PersonResponse}
import translations.application.repositories.PersonRepository
import translations.domain.model.person.{FullName, Person}

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID


/** Tests for [[PersonServiceImpl]] */
final class PersonServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private val mockRepo = mock[PersonRepository[IO]]
  // TODO: remove it, write something better.
  private val auth = new AudioPlayAuthorizationService[IO]
  private val service = new PersonServiceImpl[IO](mockRepo, auth)

  private val generatedUuid =
    UUID.fromString("00000000-0000-0000-0000-000000000001")
  private given UUIDGen[IO] with
    def randomUUID: IO[UUID] = IO.pure(generatedUuid)

  private val generated = Person.unsafe(
    id = Uuid[Person](generatedUuid),
    name = FullName.unsafe("John Smith"))

  private val person = Person.unsafe(
    id = Uuid.unsafe[Person]("9d91def4-c492-4984-b1d4-adf9a7081b56"),
    name = FullName.unsafe("John Smith"))
  private val request = PersonRequest(name = person.name)
  private val response = PersonResponse(id = person.id, name = person.name)

  private val emptyNameRequest = PersonRequest(name = "")

  "findById method " - {
    "should " - {
      "find persons if they're present in repository" in {
        // Person exists in repository.
        (mockRepo.get _).expects(person.id).returning(person.some.pure)
        for result <- service.findById(person.id)
        yield result shouldBe response.some
      }

      "not find persons if they're not present in repository" in {
        // No person found in repository.
        (mockRepo.get _).expects(person.id).returning(None.pure)
        for result <- service.findById(person.id)
        yield result shouldBe None
      }
    }
  }

  private val admin = AuthenticatedUser(
    username = "admin",
    groups = Set(Admin),
  ) // TODO: change to right checks after authorization rewrite.
  private val nonAdmin = AuthenticatedUser(
    username = "non-admin",
    groups = Set.empty,
  )

  "create method " - {
    "should " - {
      "allow users with permissions to create persons if none exist" in {
        // Persisting generated user should work OK.
        (mockRepo.persist _).expects(generated).returning(generated.pure)
        for result <- service.create(admin, request)
        yield result shouldBe PersonResponse(
          id = generated.id,
          name = generated.name).asRight
      }

      "return `BadRequest` if creating person with empty name" in {
        for result <- service.create(admin, emptyNameRequest)
        yield result shouldBe ApplicationServiceError.BadRequest.asLeft
      }

      "return `AlreadyExists` when creating person with taken identity" in {
        // Generated person already exist.
        (mockRepo.persist _)
          .expects(generated)
          .returning(RepositoryError.AlreadyExists.raiseError)
        for result <- service.create(admin, request)
        yield result shouldBe ApplicationServiceError.AlreadyExists.asLeft
      }

      "return `PermissionDenied` when users without permissions are trying to create persons" in {
        for result <- service.create(nonAdmin, request)
        yield result shouldBe ApplicationServiceError.PermissionDenied.asLeft
      }
    }
  }

  private val updated = Person.unsafe(person.id, FullName.unsafe("John Brown"))
  private val updatedRequest = PersonRequest(name = updated.name)
  private val updatedResponse = PersonResponse(person.id, name = updated.name)
  "update method " - {
    "should " - {
      "allow users with permissions to update persons if they exist" in {
        // Checking if person exist should return positive.
        (mockRepo.get _).expects(person.id).returning(person.some.pure)
        // Subsequent update should work as normal.
        (mockRepo.update _).expects(updated).returning(updated.pure)

        for result <- service.update(admin, person.id, updatedRequest)
        yield result shouldBe updatedResponse.asRight
      }

      "return `BadRequest` if updating person with empty name" in {
        // Checking if person exist should return positive.
        (mockRepo.get _).expects(person.id).returning(person.some.pure)

        for result <- service.update(admin, person.id, emptyNameRequest)
        yield result shouldBe ApplicationServiceError.BadRequest.asLeft
      }

      "return `NotFound` when updating non-existent person" in {
        // Checking if person exist should return negative.
        (mockRepo.get _).expects(person.id).returning(None.pure)
        for result <- service.update(admin, person.id, request)
        yield result shouldBe ApplicationServiceError.NotFound.asLeft
      }

      "return `PermissionDenied` when users without permissions are trying to update persons" in {
        for result <- service.update(nonAdmin, person.id, request)
        yield result shouldBe ApplicationServiceError.PermissionDenied.asLeft
      }
    }
  }

  "delete method " - {
    "should " - {
      "allow users with permissions to delete existing persons" in {
        // Checking if person exist should return positive.
        (mockRepo.delete _).expects(person.id).returning(().pure)
        for result <- service.delete(admin, person.id)
        yield result shouldBe ().asRight
      }

      "return `PermissionDenied` when users without permissions are trying to delete persons" in {
        for result <- service.delete(nonAdmin, person.id)
        yield result shouldBe ApplicationServiceError.PermissionDenied.asLeft
      }
    }
  }

end PersonServiceImplTest
