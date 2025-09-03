package org.aulune.auth
package adapters.service


import domain.model.{ExternalId, User, Username}
import domain.repositories.UserRepository
import domain.services.{BasicAuthenticationService, PasswordHashingService}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory


/** Tests for [[BasicAuthenticationServiceImpl]]. */
final class BasicAuthenticationServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private given LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val mockRepo = mock[UserRepository[IO]]
  private val mockHasher = mock[PasswordHashingService[IO]]

  private def stand(
      testCase: BasicAuthenticationService[IO] => IO[Assertion],
  ): IO[Assertion] = testCase(
    BasicAuthenticationServiceImpl(
      repo = mockRepo,
      hasher = mockHasher,
    ))

  private val user = User.unsafe(
    id = Uuid.unsafe("a18432b9-9552-4b95-8e8a-e36dba18c1ac"),
    username = Username.unsafe("username"),
    hashedPassword = Option("hash"),
    googleId = Option(ExternalId.unsafe("google_id")),
  )
  private val password = "password"

  private def mockGetByUsername(returning: IO[Option[User]]) =
    (mockRepo.getByUsername _).expects(user.username).returning(returning)
  private def mockVerifyPassword(password: String, returning: IO[Boolean]) =
    (mockHasher.verifyPassword _)
      .expects(password, user.hashedPassword.get)
      .returning(returning)

  "authenticate method " - {
    "should " - {
      "return user if everything is OK" in stand { service =>
        val _ = mockGetByUsername(user.some.pure)
        val _ = mockVerifyPassword(password, true.pure)
        for result <- service.authenticate(user.username, password)
        yield result shouldBe user.some
      }

      "return None if user with given username doesn't exist" in stand {
        service =>
          val _ = mockGetByUsername(None.pure)
          for result <- service.authenticate(user.username, password)
          yield result shouldBe None
      }

      "return None if password is not valid" in stand {
        service =>
          val _ = mockGetByUsername(user.some.pure)
          val _ = mockVerifyPassword(password, false.pure)
          for result <- service.authenticate(user.username, password)
          yield result shouldBe None
      }
    }
  }
