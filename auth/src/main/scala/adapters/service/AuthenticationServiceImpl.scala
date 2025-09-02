package org.aulune.auth
package adapters.service


import adapters.service.errors.AuthenticationServiceErrorResponses as ErrorResponses
import adapters.service.mappers.OAuth2ProviderMapper
import application.AuthenticationService
import application.dto.AuthenticateUserRequest.{
  BasicAuthentication,
  OAuth2Authentication,
}
import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  CreateUserRequest,
  UserInfo,
}
import domain.errors.{OAuthError, UserValidationError}
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

import cats.MonadThrow
import cats.data.EitherT
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.given
import org.typelevel.log4cats.{Logger, LoggerFactory}


/** [[AuthenticationService]] implementation.
 *  @param repo repository with users.
 *  @param accessTokenService service that generates and decodes token.
 *  @param idTokenService service that generates ID tokens.
 *  @param basicAuthService service to which basic authentication requests will
 *    be delegated.
 *  @param oauth2AuthService service to which OAuth2 authentication requests
 *    will be delegated.
 *  @tparam F effect type.
 */
final class AuthenticationServiceImpl[F[_]: MonadThrow: UUIDGen: LoggerFactory](
    repo: UserRepository[F],
    accessTokenService: AccessTokenService[F],
    idTokenService: IdTokenService[F],
    basicAuthService: BasicAuthenticationService[F],
    oauth2AuthService: OAuth2AuthenticationService[F],
) extends AuthenticationService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger

  override def login(
      request: AuthenticateUserRequest,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]] = (for
    _ <- eitherTLogger.info(s"Authentication request: $request")
    user <- delegateLogin(request)
    _ <- eitherTLogger.info(s"Successful login for $request")
    response <- EitherT(makeResponseForUser(user))
  yield response).value
    .handleErrorWith { e =>
      for _ <- Logger[F].error(e)("Uncaught exception.")
      yield ErrorResponses.internal.asLeft
    }

  override def register(
      request: CreateUserRequest,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]] = (for
    _ <- eitherTLogger.info(s"Registration request: $request.")
    code <- EitherT
      .fromOption(
        AuthorizationCode(request.oauth2.authorizationCode),
        ErrorResponses.invalidOAuthCode)
      .leftSemiflatTap(_ => warn"Couldn't exchange code for token.")
    provider = OAuth2ProviderMapper.toDomain(request.oauth2.provider)
    oid <- EitherT(getExternalId(provider, code))
    _ <- EitherT(checkIfRegistered(provider, oid))

    id <- EitherT.liftF(UUIDGen[F].randomUUID.map(Uuid[User]))
    user <- EitherT.fromEither(createUser(id, request.username, provider, oid))
    persisted <- EitherT(repo.persist(user).map(_.asRight).recoverWith {
      case RepositoryError.AlreadyExists =>
        for _ <- warn"Already registered user's request: $request."
        yield ErrorResponses.alreadyRegistered.asLeft
    })
    _ <- eitherTLogger.info(s"Persisted new user: $persisted.")
    response <- EitherT(makeResponseForUser(persisted))
  yield response).value
    .handleErrorWith { e =>
      for _ <- Logger[F].error(e)("Uncaught exception.")
      yield ErrorResponses.internal.asLeft
    }

  override def getUserInfo(
      accessToken: String,
  ): F[Either[ErrorResponse, UserInfo]] = (for
    _ <- eitherTLogger.info(s"User info request for token: $accessToken.")
    token <- EitherT.fromOption(
      TokenString(accessToken),
      ErrorResponses.invalidAccessToken)
    id <- EitherT.fromOptionF(
      accessTokenService.decodeAccessToken(token),
      ErrorResponses.invalidAccessToken)

    user <- EitherT.fromOptionF(repo.get(id), ErrorResponses.notRegistered)
    userInfo = UserInfo(id = user.id, username = user.username)
  yield userInfo).value
    .handleErrorWith { e =>
      for _ <- Logger[F].error(e)("Uncaught exception.")
      yield ErrorResponses.internal.asLeft
    }

  /** Makes [[AuthenticateUserResponse]] for given user.
   *  @param user user for whom response is being made.
   */
  private def makeResponseForUser(
      user: User,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]] =
    for
      accessToken <- accessTokenService.generateAccessToken(user)
      idToken <- idTokenService.generateIdToken(user)
      response = AuthenticateUserResponse(accessToken, idToken = idToken)
      _ <- info"Made response for user: $user."
    yield response.asRight

  /** Gets user ID in third-party services.
   *  @param provider chosen OAuth2 provider.
   *  @param code authorization code received from client.
   */
  private def getExternalId(
      provider: OAuth2Provider,
      code: AuthorizationCode,
  ): F[Either[ErrorResponse, ExternalId]] = oauth2AuthService
    .getId(provider, code)
    .map { either =>
      either.leftMap {
        case OAuthError.Unavailable  => ErrorResponses.externalUnavailable
        case OAuthError.Rejected     => ErrorResponses.invalidOAuthCode
        case OAuthError.InvalidToken => ErrorResponses.externalUnavailable
      }
    }

  /** Checks if user is already in repository.
   *  @param provider third-party OAuth2 provider.
   *  @param id user's ID in third-party services.
   *  @return `Unit` if user doesn't exist, otherwise error.
   */
  private def checkIfRegistered(
      provider: OAuth2Provider,
      id: ExternalId,
  ): F[Either[ErrorResponse, Unit]] = oauth2AuthService
    .findUser(provider, id)
    .map {
      case Some(user) => ErrorResponses.alreadyRegistered.asLeft
      case None       => ().asRight
    }

  /** Creates user from registration request and third-party id.
   *  @param username user chosen username.
   *  @param provider provider of OAuth services.
   *  @param oauth2Id third-party ID.
   *  @return user or NEC of errors.
   */
  private def createUser(
      id: Uuid[User],
      username: String,
      provider: OAuth2Provider,
      oauth2Id: ExternalId,
  ): Either[ErrorResponse, User] = Username(username)
    .toValidNec(UserValidationError.InvalidUsername)
    .andThen(username => User.create(id = id, username = username))
    .andThen(user => linkExternalAccount(user, provider, oauth2Id))
    .toEither
    .leftMap(ErrorResponses.invalidRegistrationDetails)

  /** Places id into user based on given provider.
   *  @param user user whose being modified.
   *  @param provider third-party OAuth2 provider.
   *  @param id third-party ID.
   *  @return validation result of final user state.
   */
  private def linkExternalAccount(
      user: User,
      provider: OAuth2Provider,
      id: ExternalId,
  ) = provider match
    case OAuth2Provider.Google => user.update(googleId = Some(id))

  /** Delegates login request to a service that can manage it.
   *  @param request login request.
   *  @return user if login is successful, otherwise `None`.
   */
  private def delegateLogin(
      request: AuthenticateUserRequest,
  ): EitherT[F, ErrorResponse, User] = request match
    case req @ BasicAuthentication(username, password) =>
      for
        username <- EitherT
          .fromOption(Username(username), ErrorResponses.invalidCredentials)
          .leftSemiflatTap(_ => warn"Login with invalid username: $request.")
        user <- EitherT
          .fromOptionF(
            basicAuthService.authenticate(username, password),
            ErrorResponses.invalidCredentials)
          .leftSemiflatTap(_ => warn"Basic authentication failed: $request.")
      yield user

    case OAuth2Authentication(providerDto, code) =>
      for
        code <- EitherT
          .fromOption(AuthorizationCode(code), ErrorResponses.invalidOAuthCode)
          .leftSemiflatTap(_ => warn"Invalid OAuth code: $request.")
        provider = OAuth2ProviderMapper.toDomain(providerDto)
        oid <- EitherT(getExternalId(provider, code))
        user <- EitherT
          .fromOptionF(
            oauth2AuthService.findUser(provider, oid),
            ErrorResponses.notRegistered)
          .leftSemiflatTap(_ =>
            warn"Unregistered user is trying to log in: $request.")
      yield user
