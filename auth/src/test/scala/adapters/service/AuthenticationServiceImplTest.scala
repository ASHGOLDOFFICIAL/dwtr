package org.aulune.auth
package adapters.service


import application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import application.dto.CreateUserRequest
import application.dto.OAuth2Provider.Google
import application.errors.UserRegistrationError.{
  InvalidOAuthCode,
  OAuthUserAlreadyExists,
}
import application.{AuthenticationService, BasicAuthenticationService}
import domain.model.{User, Username}
import domain.services.{
  AccessTokenService,
  IdTokenService,
  OAuth2AuthenticationService,
}

import cats.data.NonEmptyChain
import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.auth.domain.repositories.UserRepository
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID


/** Tests for [[AuthenticationServiceImpl]]. */
final class AuthenticationServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private val mockRepo = mock[UserRepository[IO]]
  private val mockAccess = mock[AccessTokenService[IO]]
  private val mockId = mock[IdTokenService[IO]]
  private val mockBasic = mock[BasicAuthenticationService[IO]]
  private val mockOauth = mock[OAuth2AuthenticationService[IO]]

  private val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private given UUIDGen[IO] = new UUIDGen[IO]:
    override def randomUUID: IO[UUID] = uuid.pure[IO]

  private def stand(
      testCase: AuthenticationService[IO] => IO[Assertion],
  ): IO[Assertion] = testCase(
    AuthenticationServiceImpl(
      repo = mockRepo,
      accessTokenService = mockAccess,
      idTokenService = mockId,
      basicAuthService = mockBasic,
      oauth2AuthService = mockOauth,
    ))

  private val newUser = User.unsafe(
    id = Uuid(uuid),
    username = Username.unsafe("username"),
    hashedPassword = None,
    googleId = Some("google_id"),
  )

  private val createUserRequest = CreateUserRequest(
    username = newUser.username,
    OAuth2AuthenticationRequest(
      provider = Google,
      authorizationCode = "code",
    ),
  )

  "register method " - {
    "should " - {
      "create new user if everything is OK" in stand { service =>
        // OAuth code exchange is successful.
        (mockOauth.getId _)
          .expects(
            createUserRequest.oauth2.provider,
            createUserRequest.oauth2.authorizationCode)
          .returning(newUser.googleId.pure[IO])

        // User isn't registered yet.
        (mockOauth.findUser _)
          .expects(
            createUserRequest.oauth2.provider,
            newUser.googleId.get,
          )
          .returning(None.pure[IO])

        // Persisting doesn't lead to errors.
        (mockRepo.persist _)
          .expects(newUser)
          .returning(newUser.pure[IO])

        for result <- service.register(createUserRequest)
        yield result shouldBe ().asRight
      }
    }

    "result in error if code exchange failed" in stand { service =>
      // OAuth code exchange failed.
      (mockOauth.getId _)
        .expects(
          createUserRequest.oauth2.provider,
          createUserRequest.oauth2.authorizationCode)
        .returning(None.pure[IO])

      for result <- service.register(createUserRequest)
      yield result shouldBe NonEmptyChain.one(InvalidOAuthCode).asLeft
    }

    "result in error if user's already registered" in stand { service =>
      // OAuth code exchange is successful.
      (mockOauth.getId _)
        .expects(
          createUserRequest.oauth2.provider,
          createUserRequest.oauth2.authorizationCode)
        .returning(newUser.googleId.pure[IO])

      // User is already registered.
      (mockOauth.findUser _)
        .expects(
          createUserRequest.oauth2.provider,
          newUser.googleId.get,
        )
        .returning(Some(newUser).pure[IO])

      for result <- service.register(createUserRequest)
      yield result shouldBe NonEmptyChain.one(OAuthUserAlreadyExists).asLeft
    }

    "result in error if user's already persisted" in stand { service =>
      // OAuth code exchange is successful.
      (mockOauth.getId _)
        .expects(
          createUserRequest.oauth2.provider,
          createUserRequest.oauth2.authorizationCode)
        .returning(newUser.googleId.pure[IO])

      // User isn't registered yet.
      (mockOauth.findUser _)
        .expects(
          createUserRequest.oauth2.provider,
          newUser.googleId.get,
        )
        .returning(None.pure[IO])

      // User already in repository.
      (mockRepo.persist _)
        .expects(newUser)
        .returning(IO.raiseError(RepositoryError.AlreadyExists))

      for result <- service.register(createUserRequest)
      yield result shouldBe NonEmptyChain.one(OAuthUserAlreadyExists).asLeft
    }
  }

end AuthenticationServiceImplTest
