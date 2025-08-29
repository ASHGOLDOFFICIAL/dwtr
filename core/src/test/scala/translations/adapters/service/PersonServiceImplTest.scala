package org.aulune
package translations.adapters.service


import shared.errors.ApplicationServiceError
import shared.model.Uuid
import shared.repositories.RepositoryError
import shared.service.auth.User
import shared.service.permission.PermissionClientService
import translations.application.TranslationPermission.Modify
import translations.application.dto.person.{PersonRequest, PersonResponse}
import translations.application.repositories.PersonRepository
import translations.domain.model.person.{FullName, Person}

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
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
  private val mockPermissions = mock[PermissionClientService[IO]]
  private val service = new PersonServiceImpl[IO](mockRepo, mockPermissions)

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

  private val user = User(
    id = UUID.fromString("f04eb510-229c-4cdd-bd7b-9691c3b28ae1"),
    username = "username",
  )
  "create method " - {
    "should " - {
      "allow users with permissions to create persons if none exist" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(true))
        // Persisting generated user should work OK.
        (mockRepo.persist _).expects(generated).returning(generated.pure)

        for result <- service.create(user, request)
        yield result shouldBe PersonResponse(
          id = generated.id,
          name = generated.name).asRight
      }

      "return `InvalidArgument` if creating person with empty name" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(true))

        for result <- service.create(user, emptyNameRequest)
        yield result shouldBe ApplicationServiceError.InvalidArgument.asLeft
      }

      "return `AlreadyExists` when creating person with taken identity" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(true))
        // Generated person already exist.
        (mockRepo.persist _)
          .expects(generated)
          .returning(RepositoryError.AlreadyExists.raiseError)

        for result <- service.create(user, request)
        yield result shouldBe ApplicationServiceError.AlreadyExists.asLeft
      }

      "return `PermissionDenied` when users without permissions are trying to create persons" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(false))

        for result <- service.create(user, request)
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
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(true))
        // Checking if person exist should return positive.
        (mockRepo.get _).expects(person.id).returning(person.some.pure)
        // Subsequent update should work as normal.
        (mockRepo.update _).expects(updated).returning(updated.pure)

        for result <- service.update(user, person.id, updatedRequest)
        yield result shouldBe updatedResponse.asRight
      }

      "return `InvalidArgument` if updating person with empty name" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(true))
        // Checking if person exist should return positive.
        (mockRepo.get _).expects(person.id).returning(person.some.pure)

        for result <- service.update(user, person.id, emptyNameRequest)
        yield result shouldBe ApplicationServiceError.InvalidArgument.asLeft
      }

      "return `NotFound` when updating non-existent person" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(true))
        // Checking if person exist should return negative.
        (mockRepo.get _).expects(person.id).returning(None.pure)

        for result <- service.update(user, person.id, request)
        yield result shouldBe ApplicationServiceError.NotFound.asLeft
      }

      "return `PermissionDenied` when users without permissions are trying to update persons" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(false))

        for result <- service.update(user, person.id, request)
        yield result shouldBe ApplicationServiceError.PermissionDenied.asLeft
      }
    }
  }

  "delete method " - {
    "should " - {
      "allow users with permissions to delete existing persons" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(true))
        // Checking if person exist should return positive.
        (mockRepo.delete _).expects(person.id).returning(().pure)

        for result <- service.delete(user, person.id)
        yield result shouldBe ().asRight
      }

      "return `PermissionDenied` when users without permissions are trying to delete persons" in {
        // User has permission.
        (mockPermissions.hasPermission _)
          .expects(user, Modify)
          .returning(IO(false))

        for result <- service.delete(user, person.id)
        yield result shouldBe ApplicationServiceError.PermissionDenied.asLeft
      }
    }
  }

end PersonServiceImplTest
