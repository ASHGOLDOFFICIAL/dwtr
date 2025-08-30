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
}
import application.repositories.UserRepository
import application.{
  AccessTokenService,
  AuthenticationService,
  BasicAuthenticationService,
  IdTokenService,
  OAuth2AuthenticationService,
}
import domain.model.{TokenString, User}

import cats.Monad
import cats.data.OptionT
import cats.syntax.all.given


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
final class AuthenticationServiceImpl[F[_]: Monad](
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

  override def getUserInfo(token: String): F[Option[AuthenticatedUser]] =
    TokenString(token).traverseFilter(accessTokenService.decodeAccessToken)

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
