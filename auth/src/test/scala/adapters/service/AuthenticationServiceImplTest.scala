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

import cats.Functor
import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
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

  private def mockGetId(returning: IO[Either[OAuthError, ExternalId]]) =
    (mockOauth.getId _)
      .expects(provider, authorizationCode)
      .returning(returning)

  private def mockFindUser(returning: IO[Option[User]]) = (mockOauth.findUser _)
    .expects(provider, oid)
    .returning(returning)

  private def mockPersist(returning: IO[User]) = (mockRepo.persist _)
    .expects(newUser)
    .returning(returning)

  private def mockGenerateAccessToken(returning: IO[TokenString]) =
    (mockAccess.generateAccessToken _)
      .expects(newUser)
      .returning(returning)

  private def mockGenerateIdToken(returning: IO[TokenString]) =
    (mockId.generateIdToken _)
      .expects(newUser)
      .returning(returning)

  "register method " - {
    "should " - {
      "create new user if everything is OK" in stand { service =>
        val _ = mockGetId(oid.asRight.pure[IO])
        val _ = mockFindUser(None.pure[IO])
        val _ = mockPersist(newUser.pure[IO])
        val _ = mockGenerateAccessToken(accessToken.pure[IO])
        val _ = mockGenerateIdToken(idToken.pure[IO])
        for result <- service.register(createUserRequest)
        yield result shouldBe authenticateResponse.asRight
      }

      "result in InvalidOAuthCode if authentication code was rejected" in stand {
        service =>
          val _ = mockGetId(OAuthError.Rejected.asLeft.pure[IO])
          val register = service.register(createUserRequest)
          assertFailure(register)(InvalidOAuthCode)
      }

      "result in ExternalServiceFailure when invalid token was received" in stand {
        service =>
          val _ = mockGetId(OAuthError.InvalidToken.asLeft.pure[IO])
          val register = service.register(createUserRequest)
          assertFailure(register)(ExternalServiceFailure)
      }

      "result in ExternalServiceFailure when external service is unavalible" in stand {
        service =>
          val _ = mockGetId(OAuthError.Unavailable.asLeft.pure[IO])
          val register = service.register(createUserRequest)
          assertFailure(register)(ExternalServiceFailure)
      }

      "result in UserAlreadyExists if user's already registered" in stand {
        service =>
          val _ = mockGetId(oid.asRight.pure[IO])
          val _ = mockFindUser(Some(newUser).pure[IO])
          val register = service.register(createUserRequest)
          assertFailure(register)(UserAlreadyExists)
      }

      "result in UserAlreadyExists if user's already persisted" in stand {
        service =>
          val _ = mockGetId(oid.asRight.pure[IO])
          val _ = mockFindUser(None.pure[IO])
          val _ = mockPersist(IO.raiseError(RepositoryError.AlreadyExists))
          val register = service.register(createUserRequest)
          assertFailure(register)(UserAlreadyExists)
      }

      "handle exceptions from getId gracefully" in stand { service =>
        val _ = mockGetId(IO.raiseError(new Throwable()))
        assertInternalError(service.register(createUserRequest))
      }

      "handle exceptions from findUser gracefully" in stand { service =>
        val _ = mockGetId(oid.asRight.pure[IO])
        val _ = mockFindUser(IO.raiseError(new Throwable()))
        assertInternalError(service.register(createUserRequest))
      }

      "handle exceptions from persist gracefully" in stand { service =>
        val _ = mockGetId(oid.asRight.pure[IO])
        val _ = mockFindUser(None.pure[IO])
        val _ = mockPersist(IO.raiseError(new Throwable()))
        assertInternalError(service.register(createUserRequest))
      }

      "handle exceptions from generateAccessToken gracefully" in stand {
        service =>
          val _ = mockGetId(oid.asRight.pure[IO])
          val _ = mockFindUser(None.pure[IO])
          val _ = mockPersist(newUser.pure[IO])
          val _ = mockGenerateAccessToken(IO.raiseError(new Throwable()))
          assertInternalError(service.register(createUserRequest))
      }

      "handle exceptions from generateIdToken gracefully" in stand { service =>
        val _ = mockGetId(oid.asRight.pure[IO])
        val _ = mockFindUser(None.pure[IO])
        val _ = mockPersist(newUser.pure[IO])
        val _ = mockGenerateAccessToken(accessToken.pure[IO])
        val _ = mockGenerateIdToken(IO.raiseError(new Throwable()))
        assertInternalError(service.register(createUserRequest))
      }
    }
  }

  /** Asserts that error response of given reason was returned.
   *  @param result operation whose result is asserted.
   *  @param expectedReason expected error reason.
   *  @tparam F effect type.
   */
  private def assertFailure[F[_]: Functor](result: F[Either[ErrorResponse, _]])(
      expectedReason: Any,
  ): F[Assertion] = result.map {
    case Left(err) => err.details.info.get.reason shouldBe expectedReason
    case Right(_)  => fail("Expected error response.")
  }

  /** Asserts that error response with [[Internal]] status was returned.
   *  @param result operation whose result is asserted.
   *  @tparam F effect type.
   */
  private def assertInternalError[F[_]: Functor](
      result: F[Either[ErrorResponse, _]],
  ): F[Assertion] = result.map {
    case Left(err) => err.status shouldBe Internal
    case Right(_)  => fail("Expected internal error.")
  }

end AuthenticationServiceImplTest
