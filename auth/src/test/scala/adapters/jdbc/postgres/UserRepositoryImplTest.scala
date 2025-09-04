package org.aulune.auth
package adapters.jdbc.postgres


import domain.model.{ExternalId, User, Username}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  AlreadyExists,
  FailedPrecondition,
}
import org.aulune.commons.testing.PostgresTestContainer
import org.aulune.commons.types.Uuid
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[UserRepositoryImpl]]. */
final class UserRepositoryImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with PostgresTestContainer:
  private def stand = makeStand(UserRepositoryImpl.build[IO])

  private val testUser = User.unsafe(
    id = Uuid.unsafe[User]("7690e9ab-700d-46ef-9e46-2bcce2d56ae3"),
    username = Username.unsafe("username"),
    hashedPassword = Option("test_hash"),
    googleId = Option(ExternalId.unsafe("google_id")),
  )
  private val updatedTestUser = testUser
    .update(
      username = Username.unsafe("new_username"),
      hashedPassword = Option("new_test_hash"),
    )
    .toOption
    .get

  "contains method " - {
    "should " - {
      "return false for non-existent user" in stand { repo =>
        for exists <- repo.contains(testUser.id)
        yield exists shouldBe false
      }

      "return true for existent user" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          exists <- repo.contains(testUser.id)
        yield exists shouldBe true
      }
    }
  }

  "get method " - {
    "should " - {
      "return `None` for non-existent user" in stand { repo =>
        for user <- repo.get(testUser.id)
        yield user shouldBe None
      }

      "retrieve existing users" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          user <- repo.get(testUser.id)
        yield user shouldBe Some(testUser)
      }
    }
  }

  "persist method " - {
    "should " - {
      "throw error if a user exists" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          result <- repo.persist(updatedTestUser).attempt
        yield result shouldBe Left(AlreadyExists)
      }

      "throw error when adding user with taken Google ID" in stand { repo =>
        val another = User.unsafe(
          id = Uuid.unsafe("eab28102-2ecd-4ff2-8572-58143fbe920d"),
          username = Username.unsafe("another_username"),
          hashedPassword = None,
          googleId = testUser.googleId,
        )
        for
          _ <- repo.persist(testUser)
          result <- repo.persist(another).attempt
        yield result shouldBe Left(AlreadyExists)
      }
    }
  }

  "update method" - {
    "should " - {
      "update users" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          updated <- repo.update(updatedTestUser)
        yield updated shouldBe updatedTestUser
      }

      "throw error for non-existent users" in stand { repo =>
        for updated <- repo.update(testUser).attempt
        yield updated shouldBe Left(FailedPrecondition)
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          updated <- repo.update(updatedTestUser)
          updated <- repo.update(updatedTestUser)
        yield updated shouldBe updatedTestUser
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete users" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          result <- repo.delete(testUser.id)
        yield result shouldBe ()
      }

      "be idempotent" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          _ <- repo.delete(testUser.id)
          result <- repo.delete(testUser.id)
        yield result shouldBe ()
      }
    }
  }

  "getByUsername method " - {
    "should " - {
      "return `None` for non-existent user" in stand { repo =>
        for user <- repo.getByUsername(testUser.username)
        yield user shouldBe None
      }

      "retrieve existing users" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          user <- repo.getByUsername(testUser.username)
        yield user shouldBe Some(testUser)
      }
    }
  }

  "getByGoogleId method " - {
    "should " - {
      "return `None` for non-existent user" in stand { repo =>
        for user <- repo.getByGoogleId(testUser.googleId.get)
        yield user shouldBe None
      }

      "retrieve existing users" in stand { repo =>
        for
          _ <- repo.persist(testUser)
          user <- repo.getByGoogleId(testUser.googleId.get)
        yield user shouldBe Some(testUser)
      }
    }
  }

end UserRepositoryImplTest
