package org.aulune
package auth.adapters.service

import auth.application.dto.AuthenticationRequest.OAuth2AuthenticationRequest
import auth.application.dto.{
  AuthenticationRequest,
  OAuth2Provider,
  UserRegistrationRequest,
}
import auth.application.errors.UserRegistrationError
import auth.application.errors.UserRegistrationError.*
import auth.application.repositories.UserRepository
import auth.application.{OAuth2AuthenticationService, UserService}
import auth.domain.errors.UserValidationError
import auth.domain.model.{User, Username}
import commons.errors
import commons.model.Uuid
import commons.repositories.RepositoryError

import cats.data.{EitherNec, EitherT, NonEmptyChain, Validated}
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import cats.{Monad, MonadThrow}


/** [[UserService]] implementation.
 *  @param oauth2Service [[OAuth2AuthenticationService]] implementation to work
 *    with third-party IDs.
 *  @param repo [[UserRepository]] to use.
 *  @tparam F effect type.
 */
final class UserServiceImpl[F[_]: MonadThrow: UUIDGen](
    oauth2Service: OAuth2AuthenticationService[F],
    repo: UserRepository[F],
) extends UserService[F]:

  override def register(
      request: UserRegistrationRequest,
  ): F[EitherNec[UserRegistrationError, Unit]] = (for
    oid <- getId(request.oauth2)
    _ <- checkIfRegistered(request.oauth2.provider, oid)
    id <- EitherT.liftF(UUIDGen[F].randomUUID.map(Uuid[User]))
    user <- EitherT.fromEither(
      createUser(id, request, oid).leftMap(fromUserValidation))
    _ <- EitherT(persist(user))
  yield ()).value

  /** Gets user ID in third-party services.
   *  @param oauth2Info OAuth2 provider and code
   */
  private def getId(
      oauth2Info: OAuth2AuthenticationRequest,
  ): EitherT[F, NonEmptyChain[UserRegistrationError], String] =
    val idOpt =
      oauth2Service.getId(oauth2Info.provider, oauth2Info.authorizationCode)
    EitherT.fromOptionF(idOpt, NonEmptyChain.one(InvalidOAuthCode))

  /** Checks if user is already in repository.
   *  @param provider third-party OAuth2 provider.
   *  @param id user's ID in third-party services.
   *  @return `Unit` if user doesn't exist, otherwise error.
   */
  private def checkIfRegistered(
      provider: OAuth2Provider,
      id: String,
  ): EitherT[F, NonEmptyChain[UserRegistrationError], Unit] = EitherT(
    oauth2Service.findUser(provider, id).map {
      case Some(value) => NonEmptyChain.one(OAuthUserAlreadyExists).asLeft
      case None        => Either.unit
    })

  /** Creates user from registration request and third-party id.
   *  @param request registration request.
   *  @param oauth2Id third-party ID.
   *  @return user or NEC of errors.
   */
  private def createUser(
      id: Uuid[User],
      request: UserRegistrationRequest,
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
      case OAuth2Provider.Google => User.linkGoogleId(user, id)

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
