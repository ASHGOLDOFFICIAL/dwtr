package org.aulune.auth
package adapters.service


import application.dto.AuthenticationRequest.{
  BasicAuthenticationRequest,
  OAuth2AuthenticationRequest
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
  InvalidOAuthCode,
  OAuthUserAlreadyExists,
}
import application.AuthenticationService
import domain.errors.UserValidationError
import domain.model.{TokenString, User, Username}
import domain.services.{AccessTokenService, BasicAuthenticationService, IdTokenService, OAuth2AuthenticationService}

import cats.MonadThrow
import cats.data.{EitherNec, EitherT, NonEmptyChain, OptionT}
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import org.aulune.auth.domain.repositories.UserRepository
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
  ): F[Option[AuthenticationResponse]] = (for
    user <- OptionT(delegateLogin(request))
    accessToken <- OptionT.liftF(accessTokenService.generateAccessToken(user))
    idToken <- OptionT.liftF(idTokenService.generateIdToken(user))
  yield AuthenticationResponse(
    accessToken = accessToken,
    idToken = idToken)).value

  override def register(
      request: CreateUserRequest,
  ): F[EitherNec[UserRegistrationError, Unit]] = (for
    oid <- getId(request.oauth2)
    _ <- checkIfRegistered(request.oauth2.provider, oid)
    id <- EitherT.liftF(UUIDGen[F].randomUUID.map(Uuid[User]))
    user <- EitherT.fromEither(
      createUser(id, request, oid).leftMap(fromUserValidation))
    _ <- EitherT(persist(user))
  yield ()).value

  override def getUserInfo(token: String): F[Option[AuthenticatedUser]] =
    TokenString(token).traverseFilter(accessTokenService.decodeAccessToken)

  /** Gets user ID in third-party services.
   *  @param oauth2Info OAuth2 provider and code
   */
  private def getId(
      oauth2Info: OAuth2AuthenticationRequest,
  ): EitherT[F, NonEmptyChain[UserRegistrationError], String] =
    val idOpt =
      oauth2AuthService.getId(oauth2Info.provider, oauth2Info.authorizationCode)
    EitherT.fromOptionF(idOpt, NonEmptyChain.one(InvalidOAuthCode))

  /** Checks if user is already in repository.
   *
   *  @param provider third-party OAuth2 provider.
   *  @param id user's ID in third-party services.
   *  @return `Unit` if user doesn't exist, otherwise error.
   */
  private def checkIfRegistered(
      provider: OAuth2Provider,
      id: String,
  ): EitherT[F, NonEmptyChain[UserRegistrationError], Unit] = EitherT(
    oauth2AuthService.findUser(provider, id).map {
      case Some(value) => NonEmptyChain.one(OAuthUserAlreadyExists).asLeft
      case None        => Either.unit
    })

  /** Creates user from registration request and third-party id.
   *
   *  @param request registration request.
   *  @param oauth2Id third-party ID.
   *  @return user or NEC of errors.
   */
  private def createUser(
      id: Uuid[User],
      request: CreateUserRequest,
      oauth2Id: String,
  ): EitherNec[UserValidationError, User] =
    for
      username <- Username(request.username)
        .toRight(NonEmptyChain.one(UserValidationError.InvalidUsername))
      userBase <- User(
        id = id,
        username = username,
        hashedPassword = None,
        googleId = None,
      ).toEither
      user <- linkAccount(userBase, request.oauth2.provider, oauth2Id).toEither
    yield user

  /** Persists user. If user already existed (race conditions for example), then
   *  error will be returned in left side.
   *  @param user user to persist.
   */
  private def persist(user: User): F[EitherNec[UserRegistrationError, Unit]] =
    repo
      .persist(user)
      .map(_ => Either.unit[NonEmptyChain[UserRegistrationError]])
      .recover { case e: RepositoryError.AlreadyExists.type =>
        NonEmptyChain.one(OAuthUserAlreadyExists).asLeft
      }

  /** Places id into user based on given provider.
   *  @param user user whose being modified.
   *  @param provider third-party OAuth2 provider.
   *  @param id third-party ID.
   *  @return validation result of final user state.
   */
  private def linkAccount(user: User, provider: OAuth2Provider, id: String) =
    provider match
      case OAuth2Provider.Google => user.update(googleId = Some(id))

  /** Converts NEC of [[UserValidationError]] to NEC of
   *  [[UserRegistrationError]].
   *  @param errs NEC of errors.
   */
  private def fromUserValidation(
      errs: NonEmptyChain[UserValidationError],
  ): NonEmptyChain[UserRegistrationError] = errs.map {
    case UserValidationError.InvalidUsername =>
      UserRegistrationError.InvalidUsername
  }

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
