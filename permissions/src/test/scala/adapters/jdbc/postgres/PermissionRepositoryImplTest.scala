package org.aulune.permissions
package adapters.jdbc.postgres


import domain.repositories.PermissionRepository.PermissionIdentity
import domain.{
  Permission,
  PermissionConstraint,
  PermissionDescription,
  PermissionName,
  PermissionNamespace,
}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.repositories.RepositoryError.{
  ConstraintViolation,
  FailedPrecondition,
}
import org.aulune.commons.service.auth.User
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.types.Uuid
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID


/** Tests for [[PermissionRepositoryImpl]]. */
final class PermissionRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:

  private def stand = makeStand(PermissionRepositoryImpl.build[IO])

  private val testPermission = Permission.unsafe(
    namespace = PermissionNamespace.unsafe("testing"),
    name = PermissionName.unsafe("test"),
    description = PermissionDescription.unsafe("Used for testing."),
  )
  private val updatedTestPermission = testPermission
    .update(
      description = PermissionDescription.unsafe("Updated description"),
    )
    .get
  private val testPermissionIdentity =
    PermissionIdentity(testPermission.namespace, testPermission.name)

  "contains method " - {
    "should " - {
      "return false for non-existent permission" in stand { repo =>
        for exists <- repo.contains(testPermissionIdentity)
        yield exists shouldBe false
      }

      "return true for existent permission" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          exists <- repo.contains(testPermissionIdentity)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "return `None` for non-existent permissions" in stand { repo =>
        for audio <- repo.get(testPermissionIdentity)
        yield audio shouldBe None
      }

      "retrieve existing permissions" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          audio <- repo.get(testPermissionIdentity)
        yield audio shouldBe Some(testPermission)
      }
    }
  }

  "persist method " - {
    "should " - {
      "throw error if a permission exists" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          result <- repo.persist(updatedTestPermission).attempt
        yield result shouldBe Left(
          ConstraintViolation(PermissionConstraint.UniqueId))
      }
    }
  }

  "upsert method " - {
    "should " - {
      "persist non-existent permissions" in stand { repo =>
        for result <- repo.upsert(testPermission)
        yield result shouldBe testPermission
      }

      "update existent permissions" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          result <- repo.upsert(updatedTestPermission)
        yield result shouldBe updatedTestPermission
      }
    }
  }

  "update method" - {
    "should " - {
      "update permissions" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          updated <- repo.update(updatedTestPermission)
        yield updated shouldBe updatedTestPermission
      }

      "throw error for non-existent permissions" in stand { repo =>
        for updated <- repo.update(testPermission).attempt
        yield updated shouldBe Left(FailedPrecondition)
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          updated <- repo.update(updatedTestPermission)
          updated <- repo.update(updatedTestPermission)
        yield updated shouldBe updatedTestPermission
      }
    }
  }

  private val userId =
    Uuid[User](UUID.fromString("d6acb478-b417-4a1c-842a-a2319ae7e026"))

  "delete method " - {
    "should " - {
      "delete permissions" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          result <- repo.delete(testPermissionIdentity)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          _ <- repo.delete(testPermissionIdentity)
          result <- repo.delete(testPermissionIdentity)
        yield result shouldBe ()
      }

      "revoke deleted permission from everyone" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          _ <- repo.grantPermission(userId, testPermissionIdentity)
          _ <- repo.delete(testPermissionIdentity)
          result <- repo.hasPermission(userId, testPermissionIdentity).attempt
        yield result shouldBe FailedPrecondition.asLeft
      }
    }
  }

  "hasPermission method " - {
    "should " - {
      "return true if user has required permission" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          _ <- repo.grantPermission(userId, testPermissionIdentity)
          result <- repo.hasPermission(userId, testPermissionIdentity)
        yield result shouldBe true
      }

      "return false if user doesn't have required permission" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          result <- repo.hasPermission(userId, testPermissionIdentity)
        yield result shouldBe false
      }

      "throw FailedPrecondition if permission doesn't exist" in stand { repo =>
        for result <- repo.hasPermission(userId, testPermissionIdentity).attempt
        yield result shouldBe FailedPrecondition.asLeft
      }
    }
  }

  "grantPermission method " - {
    "should " - {
      "grant existing permissions" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          _ <- repo.grantPermission(userId, testPermissionIdentity)
          result <- repo.hasPermission(userId, testPermissionIdentity)
        yield result shouldBe true
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          _ <- repo.grantPermission(userId, testPermissionIdentity)
          _ <- repo.grantPermission(userId, testPermissionIdentity)
          result <- repo.hasPermission(userId, testPermissionIdentity)
        yield result shouldBe true
      }

      "throw FailedPrecondition if permission doesn't exist" in stand { repo =>
        for result <- repo
            .grantPermission(userId, testPermissionIdentity)
            .attempt
        yield result shouldBe FailedPrecondition.asLeft
      }
    }
  }

  "revokePermission method " - {
    "should " - {
      "revoke existing permissions" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          _ <- repo.revokePermission(userId, testPermissionIdentity)
          result <- repo.hasPermission(userId, testPermissionIdentity)
        yield result shouldBe false
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(testPermission)
          _ <- repo.revokePermission(userId, testPermissionIdentity)
          _ <- repo.revokePermission(userId, testPermissionIdentity)
          result <- repo.hasPermission(userId, testPermissionIdentity)
        yield result shouldBe false
      }

      "throw FailedPrecondition if permission doesn't exist" in stand { repo =>
        for result <- repo
            .revokePermission(userId, testPermissionIdentity)
            .attempt
        yield result shouldBe FailedPrecondition.asLeft
      }
    }
  }

end PermissionRepositoryImplTest
