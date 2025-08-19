package org.aulune
package auth.adapters.service


import auth.application.dto.AuthenticationRequest.{
  BasicAuthenticationRequest,
  OAuth2AuthenticationRequest
}
import auth.application.dto.{AuthenticationRequest, AuthenticationResponse}
import auth.application.repositories.UserRepository
import auth.application.{AuthenticationService, LoginService, TokenService}
import auth.domain.model.{AuthenticatedUser, User}

import cats.Monad
import cats.data.OptionT
import cats.effect.Clock


/** [[AuthenticationService]] implementation.
 *  @param repo repository with users.
 *  @param tokenService service that generates and decodes token.
 *  @param basicLoginService service to which basic login requests will be
 *    delegated.
 *  @param oauth2LoginService service to which OAuth2 login requests will be
 *    delegated.
 *  @tparam F effect type.
 */
final class AuthenticationServiceImpl[F[_]: Monad: Clock](
    repo: UserRepository[F],
    tokenService: TokenService[F],
    basicLoginService: LoginService[F, BasicAuthenticationRequest],
    oauth2LoginService: LoginService[F, OAuth2AuthenticationRequest],
) extends AuthenticationService[F]:

  override def login(
      request: AuthenticationRequest,
  ): F[Option[AuthenticationResponse]] = (for
    user <- OptionT(delegateLogin(request))
    token <- OptionT.liftF(tokenService.generateToken(user))
  yield AuthenticationResponse(token)).value

  override def authenticate(token: String): F[Option[AuthenticatedUser]] =
    tokenService.decodeToken(token)

  /** Delegates login request to a service that can manage it.
   *  @param request login request.
   *  @return user if login is successful, otherwise `None`.
   */
  private def delegateLogin(request: AuthenticationRequest): F[Option[User]] =
    request match
      case req @ BasicAuthenticationRequest(username, password) =>
        basicLoginService.login(req)
      case req @ OAuth2AuthenticationRequest(provider, code) =>
        oauth2LoginService.login(req)
