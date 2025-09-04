package org.aulune.auth
package adapters.service


import domain.errors.OAuthError
import domain.errors.OAuthError.{
  InvalidToken,
  NotRegistered,
  Rejected,
  Unavailable,
}
import domain.model.OAuth2Provider.Google
import domain.model.{AuthorizationCode, ExternalId, User, Username}
import domain.repositories.GoogleIdSearch
import domain.services.{OAuth2AuthenticationHandler, OAuth2CodeExchanger}

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers


/** Tests for [[OAuth2AuthenticationHandlerImpl]]. */
final class OAuth2AuthenticationHandlerImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private val mockGoogleOauth = mock[OAuth2CodeExchanger[IO, Google]]
  private val mockGoogleRepo = mock[GoogleIdSearch[IO]]

  private def stand(
      testCase: OAuth2AuthenticationHandler[IO] => IO[Assertion],
  ): IO[Assertion] = testCase(
    OAuth2AuthenticationHandlerImpl(
      googleOAuth2 = mockGoogleOauth,
      googleIdSearch = mockGoogleRepo,
    ))

  private val googleId = ExternalId.unsafe("google_id")
  private val user = User.unsafe(
    id = Uuid.unsafe("a18432b9-9552-4b95-8e8a-e36dba18c1ac"),
    username = Username.unsafe("username"),
    hashedPassword = None,
    googleId = Option(googleId),
  )
  private val code = AuthorizationCode.unsafe("code")

  private def mockGoogleExchange(
      returning: IO[Either[OAuthError, ExternalId]],
  ) = (mockGoogleOauth.exchangeForId _)
    .expects(code)
    .returning(returning)

  private def mockGoogleSearch(returning: IO[Option[User]]) =
    (mockGoogleRepo.getByGoogleId _).expects(googleId).returning(returning)

  "authenticate method via Google " - {
    "should " - {
      "return user if everything is OK" in stand { service =>
        val _ = mockGoogleExchange(googleId.asRight.pure)
        val _ = mockGoogleSearch(user.some.pure)
        for result <- service.authenticate(Google, code)
        yield result shouldBe user.asRight
      }

      "result in Rejected if Google rejected code" in stand { service =>
        val _ = mockGoogleExchange(Rejected.asLeft.pure)
        for result <- service.authenticate(Google, code)
        yield result shouldBe Rejected.asLeft
      }

      "result in Unavailable if Google is unavailable" in stand { service =>
        val _ = mockGoogleExchange(Unavailable.asLeft.pure)
        for result <- service.authenticate(Google, code)
        yield result shouldBe Unavailable.asLeft
      }

      "result in InvalidToken if invalid token is received" in stand {
        service =>
          val _ = mockGoogleExchange(InvalidToken.asLeft.pure)
          for result <- service.authenticate(Google, code)
          yield result shouldBe InvalidToken.asLeft
      }

      "result in NotRegistered if user is not registered" in stand { service =>
        val _ = mockGoogleExchange(googleId.asRight.pure)
        val _ = mockGoogleSearch(None.pure)
        for result <- service.authenticate(Google, code)
        yield result shouldBe NotRegistered(googleId).asLeft
      }
    }
  }
