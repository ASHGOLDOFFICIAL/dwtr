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
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.service.auth.User
import org.aulune.commons.testing.ErrorAssertions.assertDomainError
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID


/** Tests for [[PermissionServiceImpl]]. */
final class PermissionServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private given LoggerFactory[IO] = Slf4jFactory.create

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
    val _ = (mockRepo.upsert _)
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

  private def mockUpsert(returning: IO[Permission]) = (mockRepo.upsert _)
    .expects(testPermission)
    .returning(returning)

  private def mockHasPermission(
      permission: PermissionIdentity,
      returning: IO[Boolean],
  ) = (mockRepo.hasPermission _)
    .expects(Uuid[User](user.id), permission)
    .returning(returning)

  "registerPermission method " - {
    "should " - {
      "add new permissions" in stand { service =>
        val _ = mockUpsert(testPermission.pure)
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
          val register = service.registerPermission(invalidRequest)
          assertDomainError(register)(InvalidPermission)
      }

      "be idempotent" in stand { service =>
        val _ = mockUpsert(testPermission.pure)
        val _ = mockUpsert(testPermission.pure)
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
        val _ = mockHasPermission(adminPermission, false.pure)
        val _ = mockHasPermission(testPermissionIdentity, true.pure)
        for result <- service.checkPermission(checkRequest)
        yield result shouldBe checkResponse(Granted).asRight
      }

      "deny permission to those who don't have it" in stand { service =>
        val _ = mockHasPermission(adminPermission, false.pure)
        val _ = mockHasPermission(testPermissionIdentity, false.pure)
        for result <- service.checkPermission(checkRequest)
        yield result shouldBe checkResponse(Denied).asRight
      }

      "grant permission to admin if they have it" in stand { service =>
        val _ = mockHasPermission(adminPermission, true.pure)
        val _ = mockHasPermission(testPermissionIdentity, true.pure)
        for result <- service.checkPermission(checkRequest)
        yield result shouldBe checkResponse(Granted).asRight
      }

      "grant permission to admin even if they don't have it" in stand {
        service =>
          val _ = mockHasPermission(adminPermission, true.pure)
          val _ = mockHasPermission(testPermissionIdentity, false.pure)
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
          val check = service.checkPermission(invalidRequest)
          assertDomainError(check)(InvalidPermission)
      }

      "result in PermissionNotFound if permission hadn't been registered prior" in stand {
        service =>
          val _ = mockHasPermission(
            testPermissionIdentity,
            IO.raiseError(RepositoryError.FailedPrecondition))
          val check = service.checkPermission(checkRequest)
          assertDomainError(check)(PermissionNotFound)
      }
    }
  }

end PermissionServiceImplTest
