package org.aulune
package auth.adapters.service


import auth.application.dto.AuthenticationRequest.{
  BasicAuthenticationRequest,
  OAuth2AuthenticationRequest
}
import auth.application.dto.{AuthenticationRequest, AuthenticationResponse}
import auth.application.repositories.UserRepository
import auth.application.{
  AuthenticationService,
  BasicAuthenticationService,
  OAuth2AuthenticationService,
  TokenService,
}
import auth.domain.model.{AuthenticatedUser, User}

import cats.Monad
import cats.data.OptionT


/** [[AuthenticationService]] implementation.
 *  @param repo repository with users.
 *  @param tokenService service that generates and decodes token.
 *  @param basicAuthService service to which basic authentication requests will
 *    be delegated.
 *  @param oauth2AuthService service to which OAuth2 authentication requests
 *    will be delegated.
 *  @tparam F effect type.
 */
final class AuthenticationServiceImpl[F[_]: Monad](
    repo: UserRepository[F],
    tokenService: TokenService[F],
    basicAuthService: BasicAuthenticationService[F],
    oauth2AuthService: OAuth2AuthenticationService[F],
) extends AuthenticationService[F]:

  override def login(
      request: AuthenticationRequest,
  ): F[Option[AuthenticationResponse]] = (for
    user <- OptionT(delegateLogin(request))
    token <- OptionT.liftF(tokenService.generateToken(user))
  yield AuthenticationResponse(token)).value

  override def getUserInfo(token: String): F[Option[AuthenticatedUser]] =
    tokenService.decodeToken(token)

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
