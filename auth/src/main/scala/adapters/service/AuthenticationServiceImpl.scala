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
  UserInfo,
  CreateUserRequest,
}
import application.errors.AuthenticationServiceError
import application.errors.AuthenticationServiceError.InvalidUser
import domain.errors.{OAuthError, UserValidationError}
import domain.model.OAuth2Provider.Google
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
import cats.data.{EitherT, NonEmptyChain, OptionT}
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
    _ <- eitherTLogger.info(s"Received authentication request: $request")
    user <- delegateLogin(request)
    response <- makeResponseForUser(user)
  yield response).value

  override def register(
      request: CreateUserRequest,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]] = (for
    _ <- eitherTLogger.info(s"Received registration request: $request")
    code <- EitherT
      .fromOption(
        AuthorizationCode(request.oauth2.authorizationCode),
        ErrorResponses.invalidOAuthCode)
      .leftSemiflatTap(_ => warn"Couldn't exchange code for token")
    provider = OAuth2ProviderMapper.toDomain(request.oauth2.provider)

    oid <- EitherT(getExternalId(provider, code))
      .leftSemiflatTap(_ => warn"Couldn't get user's ID")
    _ <- EitherT(checkIfRegistered(provider, oid))
      .leftSemiflatTap(_ => warn"User is already registered")

    id <- EitherT.liftF(UUIDGen[F].randomUUID.map(Uuid[User]))
    user <- EitherT.fromEither(createUser(id, request.username, provider, oid))

    persisted <- repo.persist(user).attemptT.leftSemiflatMap {
      case RepositoryError.AlreadyExists =>
        for _ <- warn"Already registered user's request: $request"
        yield ErrorResponses.alreadyRegistered
      case e =>
        for _ <- error"Unexpected error: $e"
        yield ErrorResponses.internal
    }
    response <- makeResponseForUser(persisted)
  yield response).value

  override def getUserInfo(
      accessToken: String,
  ): F[Either[ErrorResponse, UserInfo]] = (for
    token <- EitherT.fromOption(
      TokenString(accessToken),
      ErrorResponses.invalidAccessToken)
    id <- EitherT.fromOptionF(
      accessTokenService.decodeAccessToken(token),
      ErrorResponses.invalidAccessToken)
    userO <- repo.get(id).attemptT.leftSemiflatMap { e =>
      for _ <- error"Error during getting element from repo: $e"
      yield ErrorResponses.internal
    }
    user <- EitherT.fromOption(userO, ErrorResponses.notRegistered)
    userInfo = UserInfo(id = user.id, username = user.username)
  yield userInfo).value

  /** Makes [[AuthenticateUserResponse]] for given user.
   *  @param user user for whom response is being made.
   */
  private def makeResponseForUser(
      user: User,
  ): EitherT[F, ErrorResponse, AuthenticateUserResponse] = (for
    accessToken <- accessTokenService.generateAccessToken(user)
    idToken <- idTokenService.generateIdToken(user)
  yield AuthenticateUserResponse(
    accessToken = accessToken,
    idToken = idToken)).attemptT.leftSemiflatMap(e =>
    for _ <- error"Couldn't produce tokens for user: $user, error: $e"
    yield ErrorResponses.internal)

  /** Gets user ID in third-party services.
   *  @param provider chosen OAuth2 provider.
   *  @param code authorization code received from client.
   */
  private def getExternalId(
      provider: OAuth2Provider,
      code: AuthorizationCode,
  ): F[Either[ErrorResponse, ExternalId]] =
    for result <- oauth2AuthService.getId(provider, code)
    yield result.leftMap {
      case OAuthError.Unavailable  => ErrorResponses.external
      case OAuthError.Rejected     => ErrorResponses.invalidOAuthCode
      case OAuthError.InvalidToken => ErrorResponses.external
    }

  /** Checks if user is already in repository.
   *  @param provider third-party OAuth2 provider.
   *  @param id user's ID in third-party services.
   *  @return `Unit` if user doesn't exist, otherwise error.
   */
  private def checkIfRegistered(
      provider: OAuth2Provider,
      id: ExternalId,
  ): F[Either[ErrorResponse, Unit]] =
    for result <- oauth2AuthService.findUser(provider, id).attempt
    yield result.leftMap(_ => ErrorResponses.internal).flatMap {
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
    .andThen(username =>
      User(
        id = id,
        username = username,
        hashedPassword = None,
        googleId = None,
      ))
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
  ): EitherT[F, ErrorResponse, User] = (
    request match
      case req @ BasicAuthentication(username, password) =>
        for
          username <- EitherT.fromOption(
            Username(username),
            ErrorResponses.invalidCredentials)
          user <- EitherT.fromOptionF(
            basicAuthService.authenticate(username, password),
            ErrorResponses.invalidCredentials)
        yield user

      case OAuth2Authentication(providerDto, code) =>
        for
          code <- EitherT
            .fromOption(
              AuthorizationCode(code),
              ErrorResponses.invalidOAuthCode)
            .leftSemiflatTap(_ => warn"Received invalid OAuth code: $request")
          provider = OAuth2ProviderMapper.toDomain(providerDto)
          oid <- EitherT(getExternalId(provider, code))
          user <- EitherT
            .fromOptionF(
              oauth2AuthService.findUser(provider, oid),
              ErrorResponses.notRegistered)
            .leftSemiflatTap(_ =>
              warn"Unregistered user is trying to log in: $request")
        yield user
  ).leftSemiflatTap(e => error"Error while authentication: $e")
