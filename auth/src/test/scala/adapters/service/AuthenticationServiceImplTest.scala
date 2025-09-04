package org.aulune.auth
package adapters.service


import application.AuthenticationService
import application.dto.AuthenticateUserRequest.OAuth2Authentication
import application.dto.OAuth2ProviderDto.Google
import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  CreateUserRequest,
  OAuth2ProviderDto,
  UserInfo,
}
import application.errors.AuthenticationServiceError.{
  ExternalServiceFailure,
  InvalidAccessToken,
  InvalidCredentials,
  InvalidOAuthCode,
  UserAlreadyExists,
  UserNotFound,
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
  BasicAuthenticationHandler,
  IdTokenService,
  OAuth2AuthenticationHandler,
}

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.testing.ErrorAssertions.{
  assertDomainError,
  assertInternalError,
}
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
  private val mockBasic = mock[BasicAuthenticationHandler[IO]]
  private val mockOauth = mock[OAuth2AuthenticationHandler[IO]]

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
      basicAuthHandler = mockBasic,
      oauth2AuthHandler = mockOauth,
    ))

  private val providerDto = OAuth2ProviderDto.Google
  private val provider = OAuth2Provider.Google
  private val authorizationCode = AuthorizationCode.unsafe("code")
  private val oid = ExternalId.unsafe("google_id")
  private val username = Username.unsafe("username")
  private val password = "password"
  private val accessToken = TokenString.unsafe("access_token_string")
  private val idToken = TokenString.unsafe("id_token_string")

  private val user = User.unsafe(
    id = Uuid(uuid),
    username = username,
    hashedPassword = None,
    googleId = Some(oid),
  )
  private val userInfo = UserInfo(
    id = user.id,
    username = user.username,
  )

  private val createUserRequest = CreateUserRequest(
    username = user.username,
    OAuth2Authentication(
      provider = providerDto,
      authorizationCode = authorizationCode,
    ),
  )

  private val basicAuthenticateUserRequest =
    AuthenticateUserRequest.BasicAuthentication(
      username = username,
      password = password,
    )
  private val oauthAuthenticateUserRequest =
    AuthenticateUserRequest.OAuth2Authentication(
      provider = Google,
      authorizationCode = authorizationCode,
    )
  private val authenticateResponse = AuthenticateUserResponse(
    accessToken = accessToken,
    idToken = idToken,
  )

  private def mockBasicAuthenticate(returning: IO[Option[User]]) =
    (mockBasic.authenticate _)
      .expects(user.username, password)
      .returning(returning)

  private def mockOAuthAuthenticate(returning: IO[Either[OAuthError, User]]) =
    (mockOauth.authenticate _)
      .expects(provider, authorizationCode)
      .returning(returning)

  private def mockPersist(returning: IO[User]) = (mockRepo.persist _)
    .expects(user)
    .returning(returning)

  private def mockGet(returning: IO[Option[User]]) = (mockRepo.get _)
    .expects(user.id)
    .returning(returning)

  private def mockDecodeAccessToken(returning: IO[Option[Uuid[User]]]) =
    (mockAccess.decodeAccessToken _)
      .expects(accessToken)
      .returning(returning)

  private def mockGenerateAccessToken(returning: IO[TokenString]) =
    (mockAccess.generateAccessToken _)
      .expects(user)
      .returning(returning)

  private def mockGenerateIdToken(returning: IO[TokenString]) =
    (mockId.generateIdToken _)
      .expects(user)
      .returning(returning)

  "login method with basic authentication " - {
    "should " - {
      "return tokens for user if everything is OK" in stand { service =>
        val _ = mockBasicAuthenticate(user.some.pure)
        val _ = mockGenerateAccessToken(accessToken.pure)
        val _ = mockGenerateIdToken(idToken.pure)
        for result <- service.login(basicAuthenticateUserRequest)
        yield result shouldBe authenticateResponse.asRight
      }

      "return InvalidCredentials if couldn't authenticate" in stand { service =>
        val _ = mockBasicAuthenticate(None.pure)
        val login = service.login(basicAuthenticateUserRequest)
        assertDomainError(login)(InvalidCredentials)
      }

      "handle exceptions from authenticate gracefully" in stand { service =>
        val _ = mockBasicAuthenticate(IO.raiseError(new Throwable()))
        val login = service.login(basicAuthenticateUserRequest)
        assertInternalError(login)
      }

      "handle exceptions from generateAccessToken gracefully" in stand {
        service =>
          val _ = mockBasicAuthenticate(user.some.pure)
          val _ = mockGenerateAccessToken(IO.raiseError(new Throwable()))
          val login = service.login(basicAuthenticateUserRequest)
          assertInternalError(login)
      }

      "handle exceptions from generateIdToken gracefully" in stand { service =>
        val _ = mockBasicAuthenticate(user.some.pure)
        val _ = mockGenerateAccessToken(accessToken.pure)
        val _ = mockGenerateIdToken(IO.raiseError(new Throwable()))
        val login = service.login(basicAuthenticateUserRequest)
        assertInternalError(login)
      }
    }
  }

  "login method with OAuth authentication " - {
    "should " - {
      "return tokens for user if everything is OK" in stand { service =>
        val _ = mockOAuthAuthenticate(user.asRight.pure)
        val _ = mockGenerateAccessToken(accessToken.pure)
        val _ = mockGenerateIdToken(idToken.pure)
        for result <- service.login(oauthAuthenticateUserRequest)
        yield result shouldBe authenticateResponse.asRight
      }

      "result in InvalidOAuthCode if authentication code was rejected" in stand {
        service =>
          val _ = mockOAuthAuthenticate(OAuthError.Rejected.asLeft.pure)
          val login = service.login(oauthAuthenticateUserRequest)
          assertDomainError(login)(InvalidOAuthCode)
      }

      "result in ExternalServiceFailure when invalid token was received" in stand {
        service =>
          val _ = mockOAuthAuthenticate(OAuthError.InvalidToken.asLeft.pure)
          val login = service.login(oauthAuthenticateUserRequest)
          assertDomainError(login)(ExternalServiceFailure)
      }

      "result in ExternalServiceFailure when external service is unavailable" in stand {
        service =>
          val _ = mockOAuthAuthenticate(OAuthError.Unavailable.asLeft.pure)
          val login = service.login(oauthAuthenticateUserRequest)
          assertDomainError(login)(ExternalServiceFailure)
      }

      "handle exceptions from OAuthHandler gracefully" in stand { service =>
        val _ = mockOAuthAuthenticate(IO.raiseError(new Throwable()))
        val login = service.login(oauthAuthenticateUserRequest)
        assertInternalError(login)
      }

      "handle exceptions from AccessTokenService gracefully" in stand {
        service =>
          val _ = mockOAuthAuthenticate(user.asRight.pure)
          val _ = mockGenerateAccessToken(IO.raiseError(new Throwable()))
          val login = service.login(oauthAuthenticateUserRequest)
          assertInternalError(login)
      }

      "handle exceptions from IdTokenService gracefully" in stand { service =>
        val _ = mockOAuthAuthenticate(user.asRight.pure)
        val _ = mockGenerateAccessToken(accessToken.pure)
        val _ = mockGenerateIdToken(IO.raiseError(new Throwable()))
        val login = service.login(oauthAuthenticateUserRequest)
        assertInternalError(login)
      }
    }
  }

  "register method " - {
    "should " - {
      "create new user if everything is OK" in stand { service =>
        val _ = mockOAuthAuthenticate(OAuthError.NotRegistered(oid).asLeft.pure)
        val _ = mockPersist(user.pure)
        val _ = mockGenerateAccessToken(accessToken.pure)
        val _ = mockGenerateIdToken(idToken.pure)
        for result <- service.register(createUserRequest)
        yield result shouldBe authenticateResponse.asRight
      }

      "result in InvalidOAuthCode if authentication code was rejected" in stand {
        service =>
          val _ = mockOAuthAuthenticate(OAuthError.Rejected.asLeft.pure)
          val register = service.register(createUserRequest)
          assertDomainError(register)(InvalidOAuthCode)
      }

      "result in ExternalServiceFailure when invalid token was received" in stand {
        service =>
          val _ = mockOAuthAuthenticate(OAuthError.InvalidToken.asLeft.pure)
          val register = service.register(createUserRequest)
          assertDomainError(register)(ExternalServiceFailure)
      }

      "result in ExternalServiceFailure when external service is unavailable" in stand {
        service =>
          val _ = mockOAuthAuthenticate(OAuthError.Unavailable.asLeft.pure)
          val register = service.register(createUserRequest)
          assertDomainError(register)(ExternalServiceFailure)
      }

      "result in UserAlreadyExists if user's already registered" in stand {
        service =>
          val _ = mockOAuthAuthenticate(user.asRight.pure)
          val register = service.register(createUserRequest)
          assertDomainError(register)(UserAlreadyExists)
      }

      "result in UserAlreadyExists if user's already persisted" in stand {
        service =>
          val _ =
            mockOAuthAuthenticate(OAuthError.NotRegistered(oid).asLeft.pure)
          val _ = mockPersist(IO.raiseError(RepositoryError.AlreadyExists))
          val register = service.register(createUserRequest)
          assertDomainError(register)(UserAlreadyExists)
      }

      "handle exceptions from getId gracefully" in stand { service =>
        val _ = mockOAuthAuthenticate(IO.raiseError(new Throwable()))
        assertInternalError(service.register(createUserRequest))
      }

      "handle exceptions from persist gracefully" in stand { service =>
        val _ = mockOAuthAuthenticate(OAuthError.NotRegistered(oid).asLeft.pure)
        val _ = mockPersist(IO.raiseError(new Throwable()))
        assertInternalError(service.register(createUserRequest))
      }

      "handle exceptions from generateAccessToken gracefully" in stand {
        service =>
          val _ =
            mockOAuthAuthenticate(OAuthError.NotRegistered(oid).asLeft.pure)
          val _ = mockPersist(user.pure)
          val _ = mockGenerateAccessToken(IO.raiseError(new Throwable()))
          assertInternalError(service.register(createUserRequest))
      }

      "handle exceptions from generateIdToken gracefully" in stand { service =>
        val _ = mockOAuthAuthenticate(OAuthError.NotRegistered(oid).asLeft.pure)
        val _ = mockPersist(user.pure)
        val _ = mockGenerateAccessToken(accessToken.pure)
        val _ = mockGenerateIdToken(IO.raiseError(new Throwable()))
        assertInternalError(service.register(createUserRequest))
      }
    }
  }

  "getUserInfo method" - {
    "should " - {
      "return user info if everything is OK" in stand { service =>
        val _ = mockDecodeAccessToken(user.id.some.pure)
        val _ = mockGet(user.some.pure)
        for result <- service.getUserInfo(accessToken)
        yield result shouldBe userInfo.asRight
      }

      "result in InvalidAccessToken if token cannot be decoded" in stand {
        service =>
          val _ = mockDecodeAccessToken(None.pure)
          val userInfo = service.getUserInfo(accessToken)
          assertDomainError(userInfo)(InvalidAccessToken)
      }

      "result in UserNotFound if token owner cannot be found" in stand {
        service =>
          val _ = mockDecodeAccessToken(user.id.some.pure)
          val _ = mockGet(None.pure)
          val userInfo = service.getUserInfo(accessToken)
          assertDomainError(userInfo)(UserNotFound)
      }

      "handle exceptions from decodeAccessToken gracefully" in stand {
        service =>
          val _ = mockDecodeAccessToken(IO.raiseError(new Throwable()))
          val userInfo = service.getUserInfo(accessToken)
          assertInternalError(userInfo)
      }

      "handle exceptions from repo.get gracefully" in stand { service =>
        val _ = mockDecodeAccessToken(user.id.some.pure)
        val _ = mockGet(IO.raiseError(new Throwable()))
        val userInfo = service.getUserInfo(accessToken)
        assertInternalError(userInfo)
      }
    }
  }

end AuthenticationServiceImplTest
