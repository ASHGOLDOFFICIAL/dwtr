package org.aulune.permissions
package adapters.service


import application.PermissionService
import application.dto.CheckPermissionStatus.{Denied, Granted}
import application.dto.{
  CheckPermissionRequest,
  CheckPermissionResponse,
  CheckPermissionStatus,
  CreatePermissionRequest,
  PermissionResource,
}
import application.errors.PermissionServiceError.{
  InvalidPermission,
  PermissionNotFound,
}
import domain.repositories.PermissionRepository
import domain.repositories.PermissionRepository.PermissionIdentity
import domain.{
  Permission,
  PermissionDescription,
  PermissionName,
  PermissionNamespace,
}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorInfo
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID


/** Tests for [[PermissionServiceImpl]]. */
final class PermissionServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:
  private given Logger[IO] = Slf4jLogger.getLogger[IO]

  private val domain = "org.aulune.permissions"

  private val adminNamespace = "adminNamespace"
  private val adminName = "adminName"
  private val adminPermission = PermissionIdentity(
    namespace = PermissionNamespace.unsafe(adminNamespace),
    name = PermissionName.unsafe(adminName),
  )
  private def hasAdminPermissionIdentity(p: Permission): Boolean =
    p.namespace == adminNamespace && p.name == adminName

  private val mockRepo = mock[PermissionRepository[IO]]
  private def stand(
      testCase: PermissionService[IO] => IO[Assertion],
  ): IO[Assertion] =
    (mockRepo.upsert _)
      .expects(where(hasAdminPermissionIdentity))
      .onCall(p => IO.pure(p))
    PermissionServiceImpl
      .build[IO](adminNamespace, adminName, mockRepo)
      .flatMap(testCase)

  private val user = User(
    id = UUID.fromString("e05d2bd1-f347-4861-ac53-f2d36a6f942f"),
    username = "username")

  private val testPermission = Permission.unsafe(
    namespace = PermissionNamespace.unsafe("testing"),
    name = PermissionName.unsafe("test"),
    description = PermissionDescription.unsafe("Used for testing."),
  )
  private val testPermissionIdentity = PermissionIdentity(
    namespace = testPermission.namespace,
    name = testPermission.name,
  )
  private val permissionResource = PermissionResource(
    namespace = testPermission.namespace,
    name = testPermission.name,
    description = testPermission.description)
  private val createRequest = CreatePermissionRequest(
    namespace = testPermission.namespace,
    name = testPermission.name,
    description = testPermission.description,
  )

  private val checkRequest = CheckPermissionRequest(
    namespace = testPermission.namespace,
    permission = testPermission.name,
    user = user.id,
  )
  private def checkResponse(status: CheckPermissionStatus) =
    CheckPermissionResponse(
      status = status,
      user = user.id,
      namespace = checkRequest.namespace,
      permission = checkRequest.permission,
    )

  "registerPermission method " - {
    "should " - {
      "add new permissions" in stand { service =>
        (mockRepo.upsert _)
          .expects(testPermission)
          .returning(testPermission.pure)
        for result <- service.registerPermission(createRequest)
        yield result shouldBe permissionResource.asRight
      }

      "result in InvalidPermission when request is invalid" in stand {
        service =>
          val invalidRequest = CreatePermissionRequest(
            namespace = "whitespace inside",
            name = "the same",
            description = "",
          )
          for result <- service.registerPermission(invalidRequest)
          yield result match
            case Left(error) => error.details.info shouldBe ErrorInfo(
                reason = InvalidPermission,
                domain = domain,
              ).some
            case Right(value) => fail("Error was expected.")
      }

      "be idempotent" in stand { service =>
        (mockRepo.upsert _)
          .expects(testPermission)
          .returning(testPermission.pure)
        (mockRepo.upsert _)
          .expects(testPermission)
          .returning(testPermission.pure)

        for
          _ <- service.registerPermission(createRequest)
          result <- service.registerPermission(createRequest)
        yield result shouldBe permissionResource.asRight
      }
    }
  }

  "checkPermission method " - {
    "should " - {
      "grant permission to those who have it" in stand { service =>
        // Not an admin...
        (mockRepo.hasPermission _)
          .expects(Uuid[User](user.id), adminPermission)
          .returning(false.pure)
        // ...but has a permission.
        (mockRepo.hasPermission _)
          .expects(Uuid[User](user.id), testPermissionIdentity)
          .returning(true.pure)

        for result <- service.checkPermission(checkRequest)
        yield result shouldBe checkResponse(Granted).asRight
      }

      "deny permission to those who don't have it" in stand { service =>
        // Not an admin...
        (mockRepo.hasPermission _)
          .expects(Uuid[User](user.id), adminPermission)
          .returning(false.pure)
        // ...and doesn't have a permission either.
        (mockRepo.hasPermission _)
          .expects(Uuid[User](user.id), testPermissionIdentity)
          .returning(false.pure)

        for result <- service.checkPermission(checkRequest)
        yield result shouldBe checkResponse(Denied).asRight
      }

      "grant permission to admin if they have it" in stand { service =>
        // User is an admin...
        (mockRepo.hasPermission _)
          .expects(Uuid[User](user.id), adminPermission)
          .returning(true.pure)
        // ...and does have a required permission.
        (mockRepo.hasPermission _)
          .expects(Uuid[User](user.id), testPermissionIdentity)
          .returning(true.pure)

        for result <- service.checkPermission(checkRequest)
        yield result shouldBe checkResponse(Granted).asRight
      }

      "grant permission to admin even if they don't have it" in stand {
        service =>
          // User is an admin...
          (mockRepo.hasPermission _)
            .expects(Uuid[User](user.id), adminPermission)
            .returning(true.pure)
          // ...but doesn't have a required permission.
          (mockRepo.hasPermission _)
            .expects(Uuid[User](user.id), testPermissionIdentity)
            .returning(false.pure)

          for result <- service.checkPermission(checkRequest)
          yield result shouldBe checkResponse(Granted).asRight
      }

      "result in InvalidPermission when trying to check invalid permission" in stand {
        service =>
          val invalidRequest = CheckPermissionRequest(
            namespace = "whitespace inside",
            permission = "the same",
            user = user.id,
          )
          for result <- service.checkPermission(invalidRequest)
          yield result match
            case Left(error) => error.details.info shouldBe ErrorInfo(
                reason = InvalidPermission,
                domain = domain,
              ).some
            case Right(value) => fail("Error was expected.")
      }

      "result in PermissionNotFound if permission hadn't been registered prior" in stand {
        service =>
          // User doesn't have a required permission.
          (mockRepo.hasPermission _)
            .expects(Uuid[User](user.id), testPermissionIdentity)
            .returning(IO.raiseError(RepositoryError.FailedPrecondition))

          for result <- service.checkPermission(checkRequest)
          yield result match
            case Left(error) => error.details.info shouldBe ErrorInfo(
                reason = PermissionNotFound,
                domain = domain,
              ).some
            case Right(value) => fail("Error was expected.")
      }
    }
  }

end PermissionServiceImplTest
