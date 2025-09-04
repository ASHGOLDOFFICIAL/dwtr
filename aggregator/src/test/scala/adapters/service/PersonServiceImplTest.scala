package org.aulune.aggregator
package adapters.service


import application.AggregatorPermission.Modify
import application.PersonService
import application.dto.person.{CreatePersonRequest, PersonResource}
import application.errors.PersonServiceError.{InvalidPerson, PersonNotFound}
import application.repositories.PersonRepository
import domain.model.person.{FullName, Person}

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.PermissionDenied
import org.aulune.commons.errors.{ErrorResponse, ErrorStatus}
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.{
  Permission,
  PermissionClientService,
}
import org.aulune.commons.testing.ErrorAssertions.{
  assertDomainError,
  assertErrorStatus,
  assertInternalError,
}
import org.aulune.commons.testing.instances.UUIDGenInstances.makeFixedUuidGen
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID


/** Tests for [[PersonServiceImpl]]. */
final class PersonServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private given LoggerFactory[IO] = Slf4jFactory.create

  private val mockRepo = mock[PersonRepository[IO]]
  private val mockPermissions = mock[PermissionClientService[IO]]

  private val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private given UUIDGen[IO] = makeFixedUuidGen(uuid)

  private val user = User(
    id = UUID.fromString("f04eb510-229c-4cdd-bd7b-9691c3b28ae1"),
    username = "username",
  )

  private def stand(
      testCase: PersonService[IO] => IO[Assertion],
  ): IO[Assertion] =
    val _ = (mockPermissions.registerPermission _)
      .expects(*)
      .returning(().asRight.pure)
    PersonServiceImpl
      .build(mockRepo, mockPermissions)
      .flatMap(testCase)
  end stand

  private val person = Person.unsafe(
    id = Uuid.unsafe[Person]("9d91def4-c492-4984-b1d4-adf9a7081b56"),
    name = FullName.unsafe("John Smith"))
  private val personResponse =
    PersonResource(id = person.id, name = person.name)

  private val createRequest = CreatePersonRequest(name = person.name)
  private val newPerson = Person
    .unsafe(id = Uuid[Person](uuid), name = FullName.unsafe("John Smith"))
  private val newPersonResponse =
    PersonResource(id = newPerson.id, name = newPerson.name)

  private def mockPersist(returning: IO[Person]) =
    (mockRepo.persist _).expects(newPerson).returning(returning)

  private def mockGet(returning: IO[Option[Person]]) =
    (mockRepo.get _).expects(person.id).returning(returning)

  private def mockDelete(returning: IO[Unit]) =
    (mockRepo.delete _).expects(person.id).returning(returning)

  private def mockHasPermission(
      permission: Permission,
      returning: IO[Either[ErrorResponse, Boolean]],
  ) = (mockPermissions.hasPermission _)
    .expects(user, permission)
    .returning(returning)

  "findById method " - {
    "should " - {
      "find persons if they're present in repository" in stand { service =>
        val _ = mockGet(person.some.pure)
        for result <- service.findById(person.id)
        yield result shouldBe personResponse.asRight
      }

      "result in PersonNotFound if person doesn't exist" in stand { service =>
        val _ = mockGet(None.pure)
        val find = service.findById(person.id)
        assertDomainError(find)(PersonNotFound)
      }

      "handle errors from repository gracefully" in stand { service =>
        val _ = mockGet(IO.raiseError(new Throwable()))
        val find = service.findById(person.id)
        assertInternalError(find)
      }
    }
  }

  "create method " - {
    "should " - {
      "allow users with permissions to create persons if none exist" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockPersist(newPerson.pure)
          for result <- service.create(user, createRequest)
          yield result shouldBe newPersonResponse.asRight
      }

      "result in InvalidArgument when creating person with empty name" in stand {
        service =>
          val emptyNameRequest = CreatePersonRequest(name = "")
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val find = service.create(user, emptyNameRequest)
          assertDomainError(find)(InvalidPerson)
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val find = service.create(user, createRequest)
        assertErrorStatus(find)(PermissionDenied)
      }

      "handle exceptions from hasPermission gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
        val find = service.create(user, createRequest)
        assertInternalError(find)
      }

      "handle exceptions from persist gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockPersist(IO.raiseError(new Throwable()))
        val find = service.create(user, createRequest)
        assertInternalError(find)
      }
    }
  }

  "delete method " - {
    "should " - {
      "allow users with permissions to delete existing persons" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockDelete(().pure)
          for result <- service.delete(user, person.id)
          yield result shouldBe ().asRight
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val delete = service.delete(user, person.id)
        assertErrorStatus(delete)(PermissionDenied)
      }

      "handle exceptions from hasPermission gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
        val delete = service.delete(user, person.id)
        assertInternalError(delete)
      }

      "handle exceptions from delete gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockDelete(IO.raiseError(new Throwable()))
        val delete = service.delete(user, person.id)
        assertInternalError(delete)
      }
    }
  }

end PersonServiceImplTest
