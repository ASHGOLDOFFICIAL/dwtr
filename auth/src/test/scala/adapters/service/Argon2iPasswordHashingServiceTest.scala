package org.aulune.auth
package adapters.service


import domain.services.PasswordHashingService

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[Argon2iPasswordHashingService]]. */
final class Argon2iPasswordHashingServiceTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers:

  private def stand(
      testCase: PasswordHashingService[IO] => IO[Assertion],
  ): IO[Assertion] = Argon2iPasswordHashingService.build[IO].flatMap(testCase)

  private val password = "password"

  "hashing service via argon2i " - {
    "should " - {
      "hash and verify passwords" in stand { service =>
        for
          hash <- service.hashPassword(password)
          valid <- service.verifyPassword(password, hash)
        yield valid shouldBe true
      }

      "mark password as invalid when given wrong password" in stand { service =>
        for
          hash <- service.hashPassword(password)
          valid <- service.verifyPassword("wrong_password", hash)
        yield valid shouldBe false
      }

      "mark password as invalid when given invalid hash" in stand { service =>
        for valid <- service.verifyPassword(password, "bad")
        yield valid shouldBe false
      }
    }
  }
