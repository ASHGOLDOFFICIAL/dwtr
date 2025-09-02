package org.aulune.auth
package adapters.service


import application.AuthenticationService
import application.dto.AuthenticateUserRequest.OAuth2Authentication
import application.dto.{
  AuthenticateUserResponse,
  CreateUserRequest,
  OAuth2ProviderDto,
}
import application.errors.AuthenticationServiceError.{
  ExternalServiceFailure,
  InvalidOAuthCode,
  UserAlreadyExists,
}
import domain.errors.OAuthError
import domain.model.{
  AuthorizationCode,
  ExternalId,
  OAuth2Provider,
  TokenString,
  User,
  Username,
}
import domain.repositories.UserRepository
import domain.services.{
  AccessTokenService,
  BasicAuthenticationService,
  IdTokenService,
  OAuth2AuthenticationService,
}

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.Internal
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID


/** Tests for [[AuthenticationServiceImpl]]. */
final class AuthenticationServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:
  private given LoggerFactory[IO] = Slf4jFactory.create[IO]

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

  private val providerDto = OAuth2ProviderDto.Google
  private val provider = OAuth2Provider.Google
  private val authorizationCode = AuthorizationCode.unsafe("code")
  private val oid = ExternalId.unsafe("google_id")

  private val newUser = User.unsafe(
    id = Uuid(uuid),
    username = Username.unsafe("username"),
    hashedPassword = None,
    googleId = Some(oid),
  )

  private val createUserRequest = CreateUserRequest(
    username = newUser.username,
    OAuth2Authentication(
      provider = providerDto,
      authorizationCode = authorizationCode,
    ),
  )

  private val accessToken = TokenString.unsafe("access_token_string")
  private val idToken = TokenString.unsafe("id_token_string")
  private val authenticateResponse = AuthenticateUserResponse(
    accessToken = accessToken,
    idToken = idToken,
  )

  "register method " - {
    "should " - {
      "create new user if everything is OK" in stand { service =>
        // OAuth code exchange is successful.
        val _ = (mockOauth.getId _)
          .expects(provider, authorizationCode)
          .returning(oid.asRight.pure[IO])

        // User isn't registered yet.
        val _ = (mockOauth.findUser _)
          .expects(provider, oid)
          .returning(None.pure[IO])

        // Persisting doesn't lead to errors.
        val _ = (mockRepo.persist _)
          .expects(newUser)
          .returning(newUser.pure[IO])

        // Tokens are successfully generated.
        val _ = (mockAccess.generateAccessToken _)
          .expects(newUser)
          .returning(accessToken.pure[IO])
        val _ = (mockId.generateIdToken _)
          .expects(newUser)
          .returning(idToken.pure[IO])

        for result <- service.register(createUserRequest)
        yield result shouldBe authenticateResponse.asRight
      }

      "result in InvalidOAuthCode if authentication code was rejected" in stand {
        service =>
          val _ = (mockOauth.getId _)
            .expects(provider, authorizationCode)
            .returning(OAuthError.Rejected.asLeft.pure[IO])

          for result <- service.register(createUserRequest)
          yield result match
            case Left(err) =>
              err.details.info.get.reason shouldBe InvalidOAuthCode
            case Right(value) => fail("Error was expected.")
      }

      "result in ExternalServiceFailure when invalid token was received" in stand {
        service =>
          val _ = (mockOauth.getId _)
            .expects(provider, authorizationCode)
            .returning(OAuthError.InvalidToken.asLeft.pure[IO])

          for result <- service.register(createUserRequest)
          yield result match
            case Left(err) =>
              err.details.info.get.reason shouldBe ExternalServiceFailure
            case Right(value) => fail("Error was expected.")
      }

      "result in ExternalServiceFailure when external service is unavalible" in stand {
        service =>
          val _ = (mockOauth.getId _)
            .expects(provider, authorizationCode)
            .returning(OAuthError.Unavailable.asLeft.pure[IO])

          for result <- service.register(createUserRequest)
          yield result match
            case Left(err) =>
              err.details.info.get.reason shouldBe ExternalServiceFailure
            case Right(value) => fail("Error was expected.")
      }

      "result in UserAlreadyExists if user's already registered" in stand {
        service =>
          // OAuth code exchange is successful.
          val _ = (mockOauth.getId _)
            .expects(provider, authorizationCode)
            .returning(oid.asRight.pure[IO])

          // User is already registered.
          val _ = (mockOauth.findUser _)
            .expects(provider, oid)
            .returning(Some(newUser).pure[IO])

          for result <- service.register(createUserRequest)
          yield result match
            case Left(err) =>
              err.details.info.get.reason shouldBe UserAlreadyExists
            case Right(value) => fail("Error was expected.")
      }

      "result in UserAlreadyExists if user's already persisted" in stand {
        service =>
          // OAuth code exchange is successful.
          val _ = (mockOauth.getId _)
            .expects(provider, authorizationCode)
            .returning(oid.asRight.pure[IO])

          // User isn't registered yet.
          val _ = (mockOauth.findUser _)
            .expects(provider, oid)
            .returning(None.pure[IO])

          // User already in repository.
          val _ = (mockRepo.persist _)
            .expects(newUser)
            .returning(IO.raiseError(RepositoryError.AlreadyExists))

          for result <- service.register(createUserRequest)
          yield result match
            case Left(err) =>
              err.details.info.get.reason shouldBe UserAlreadyExists
            case Right(value) => fail("Error was expected.")
      }

      "handle exceptions from getId gracefully" in stand { service =>
        val _ = (mockOauth.getId _)
          .expects(provider, authorizationCode)
          .returning(IO.raiseError(new Throwable()))

        for result <- service.register(createUserRequest)
        yield result match
          case Left(error)  => error.status shouldBe Internal
          case Right(value) => fail("Error was expected.")
      }

      "handle exceptions from findUser gracefully" in stand { service =>
        // OAuth code exchange is successful.
        val _ = (mockOauth.getId _)
          .expects(provider, authorizationCode)
          .returning(oid.asRight.pure[IO])

        val _ = (mockOauth.findUser _)
          .expects(provider, oid)
          .returning(IO.raiseError(new Throwable()))

        for result <- service.register(createUserRequest)
        yield result match
          case Left(error)  => error.status shouldBe Internal
          case Right(value) => fail("Error was expected.")
      }

      "handle exceptions from persist gracefully" in stand { service =>
        // OAuth code exchange is successful.
        val _ = (mockOauth.getId _)
          .expects(provider, authorizationCode)
          .returning(oid.asRight.pure[IO])

        // User isn't registered yet.
        val _ = (mockOauth.findUser _)
          .expects(provider, oid)
          .returning(None.pure[IO])

        val _ = (mockRepo.persist _)
          .expects(newUser)
          .returning(IO.raiseError(new Throwable()))

        for result <- service.register(createUserRequest)
        yield result match
          case Left(error)  => error.status shouldBe Internal
          case Right(value) => fail("Error was expected.")
      }

      "handle exceptions from generateAccessToken gracefully" in stand { service =>
        // OAuth code exchange is successful.
        val _ = (mockOauth.getId _)
          .expects(provider, authorizationCode)
          .returning(oid.asRight.pure[IO])

        // User isn't registered yet.
        val _ = (mockOauth.findUser _)
          .expects(provider, oid)
          .returning(None.pure[IO])

        // Persisting doesn't lead to errors.
        val _ = (mockRepo.persist _)
          .expects(newUser)
          .returning(newUser.pure[IO])

        val _ = (mockAccess.generateAccessToken _)
          .expects(newUser)
          .returning(IO.raiseError(new Throwable()))

        for result <- service.register(createUserRequest)
          yield result match
            case Left(error) => error.status shouldBe Internal
            case Right(value) => fail("Error was expected.")
      }

      "handle exceptions from generateIdToken gracefully" in stand { service =>
        // OAuth code exchange is successful.
        val _ = (mockOauth.getId _)
          .expects(provider, authorizationCode)
          .returning(oid.asRight.pure[IO])

        // User isn't registered yet.
        val _ = (mockOauth.findUser _)
          .expects(provider, oid)
          .returning(None.pure[IO])

        // Persisting doesn't lead to errors.
        val _ = (mockRepo.persist _)
          .expects(newUser)
          .returning(newUser.pure[IO])

        val _ = (mockAccess.generateAccessToken _)
          .expects(newUser)
          .returning(accessToken.pure[IO])
        val _ = (mockId.generateIdToken _)
          .expects(newUser)
          .returning(IO.raiseError(new Throwable()))

        for result <- service.register(createUserRequest)
          yield result match
            case Left(error) => error.status shouldBe Internal
            case Right(value) => fail("Error was expected.")
      }
    }
  }

end AuthenticationServiceImplTest
