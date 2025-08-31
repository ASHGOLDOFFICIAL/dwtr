package org.aulune.auth
package adapters.service


import adapters.service.errors.AuthenticationServiceErrorResponses as ErrorResponses
import application.AuthenticationService
import application.dto.AuthenticationRequest.{
  BasicAuthenticationRequest,
  OAuth2AuthenticationRequest,
}
import application.dto.{
  AuthenticatedUser,
  AuthenticationRequest,
  AuthenticationResponse,
  CreateUserRequest,
  OAuth2Provider,
}
import application.errors.UserRegistrationError
import application.errors.UserRegistrationError.{
  InvalidDetails,
  InvalidOAuthCode,
  UserAlreadyExists,
}
import domain.errors.UserValidationError
import domain.model.{TokenString, User, Username}
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
import org.aulune.commons.errors.ErrorStatus.{
  AlreadyExists,
  Internal,
  InvalidArgument,
  Unauthenticated,
}
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.errors.ErrorInfo
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.types.Uuid


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
final class AuthenticationServiceImpl[F[_]: MonadThrow: UUIDGen](
    repo: UserRepository[F],
    accessTokenService: AccessTokenService[F],
    idTokenService: IdTokenService[F],
    basicAuthService: BasicAuthenticationService[F],
    oauth2AuthService: OAuth2AuthenticationService[F],
) extends AuthenticationService[F]:

  override def login(
      request: AuthenticationRequest,
  ): F[Either[ErrorResponse, AuthenticationResponse]] = (for
    user <- EitherT.fromOptionF(
      delegateLogin(request),
      ErrorResponses.failedLoginResponse)
    response <- EitherT.liftF(makeResponseForUser(user))
  yield response).value

  override def register(
      request: CreateUserRequest,
  ): F[Either[ErrorResponse, AuthenticationResponse]] = (for
    oid <- EitherT.fromOptionF(
      getId(request.oauth2),
      ErrorResponses.failedToRetrieveId)
    _ <- EitherT(checkIfRegistered(request.oauth2.provider, oid))
    id <- EitherT.liftF(UUIDGen[F].randomUUID.map(Uuid[User]))
    user <- EitherT.fromEither(createUser(id, request, oid))
    _ <- EitherT(persist(user))
    response <- EitherT.liftF(makeResponseForUser(user))
  yield response).value

  override def getUserInfo(
      accessToken: String,
  ): F[Either[ErrorResponse, AuthenticatedUser]] = (for
    token <- OptionT.fromOption(TokenString(accessToken))
    user <- OptionT(accessTokenService.decodeAccessToken(token))
  yield user).toRight(ErrorResponses.invalidAccessToken).value

  /** Makes [[AuthenticationResponse]] for given user.
   *  @param user user for whom response is being made.
   */
  private def makeResponseForUser(user: User): F[AuthenticationResponse] =
    for
      accessToken <- accessTokenService.generateAccessToken(user)
      idToken <- idTokenService.generateIdToken(user)
    yield AuthenticationResponse(accessToken = accessToken, idToken = idToken)

  /** Gets user ID in third-party services.
   *  @param oauth2Info OAuth2 provider and code
   */
  private def getId(
      oauth2Info: OAuth2AuthenticationRequest,
  ): F[Option[String]] =
    oauth2AuthService.getId(oauth2Info.provider, oauth2Info.authorizationCode)

  /** Checks if user is already in repository.
   *  @param provider third-party OAuth2 provider.
   *  @param id user's ID in third-party services.
   *  @return `Unit` if user doesn't exist, otherwise error.
   */
  private def checkIfRegistered(
      provider: OAuth2Provider,
      id: String,
  ): F[Either[ErrorResponse, Unit]] =
    for result <- oauth2AuthService.findUser(provider, id).attempt
    yield result.leftMap(_ => ErrorResponses.internalError).flatMap {
      case Some(user) => ErrorResponses.alreadyRegistered.asLeft
      case None       => ().asRight
    }

  /** Creates user from registration request and third-party id.
   *  @param request registration request.
   *  @param oauth2Id third-party ID.
   *  @return user or NEC of errors.
   */
  private def createUser(
      id: Uuid[User],
      request: CreateUserRequest,
      oauth2Id: String,
  ): Either[ErrorResponse, User] = Username(request.username)
    .toValidNec(UserValidationError.InvalidUsername)
    .andThen(username =>
      User(
        id = id,
        username = username,
        hashedPassword = None,
        googleId = None,
      ))
    .andThen(user => linkAccount(user, request.oauth2.provider, oauth2Id))
    .toEither
    .leftMap(ErrorResponses.invalidRegistrationDetails)

  /** Places id into user based on given provider.
   *  @param user user whose being modified.
   *  @param provider third-party OAuth2 provider.
   *  @param id third-party ID.
   *  @return validation result of final user state.
   */
  private def linkAccount(user: User, provider: OAuth2Provider, id: String) =
    provider match
      case OAuth2Provider.Google => user.update(googleId = Some(id))

  /** Persists user. If user already existed (race conditions for example), then
   *  error will be returned in left side.
   *  @param user user to persist.
   */
  private def persist(user: User): F[Either[ErrorResponse, User]] =
    for result <- repo.persist(user).attempt
    yield result.leftMap {
      case RepositoryError.AlreadyExists => ErrorResponses.alreadyRegistered
      case _                             => ErrorResponses.internalError
    }

  /** Converts NEC of [[UserValidationError]] to NEC of
   *  [[UserRegistrationError]].
   *  @param errs NEC of errors.
   */
  private def fromUserValidation(
      errs: NonEmptyChain[UserValidationError],
  ): NonEmptyChain[UserRegistrationError] =
    errs.map { case UserValidationError.InvalidUsername => InvalidDetails }

  /** Delegates login request to a service that can manage it.
   *  @param request login request.
   *  @return user if login is successful, otherwise `None`.
   */
  private def delegateLogin(request: AuthenticationRequest): F[Option[User]] =
    request match
      case req @ BasicAuthenticationRequest(username, password) =>
        basicAuthService.authenticate(req)
      case req @ OAuth2AuthenticationRequest(provider, code) =>
        oauth2AuthService.authenticate(req)
