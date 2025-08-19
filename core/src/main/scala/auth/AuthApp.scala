package org.aulune
package auth


import auth.adapters.jdbc.postgres.UserRepositoryImpl
import auth.adapters.service.{
  Argon2iPasswordHashingService,
  AuthenticationServiceImpl,
  BasicLoginService,
  GoogleOAuth2AuthenticationService,
  JwtTokenService,
  OAuth2AuthenticationFacade,
  OAuth2LoginService,
  UserServiceImpl,
}
import auth.api.http.{AuthenticationController, UsersController}
import shared.auth.AuthenticationService

import cats.effect.Async
import cats.syntax.all.*
import doobie.Transactor
import org.http4s.ember.client.EmberClientBuilder
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.duration.DurationInt


/** Authentication app with Tapir endpoints and client-side service
 *  implementation.
 *  @tparam F effect type.
 */
trait AuthApp[F[_]]:
  val clientAuthentication: AuthenticationService[F]
  val endpoints: List[ServerEndpoint[Any, F]]


object AuthApp:
  /** Builds auth app. It's used to hide authentication wiring logic.
   *  @param config application config.
   *  @param transactor transactor for DB.
   *  @tparam F effect type.
   */
  def build[F[_]: Async](
      config: AuthConfig,
      transactor: Transactor[F],
  ): F[AuthApp[F]] = EmberClientBuilder.default[F].build.use { httpClient =>
    for
      userRepo <- UserRepositoryImpl.build[F](transactor)

      googleAuth <-
        GoogleOAuth2AuthenticationService.build(config.oauth.google, httpClient)
      oauthFacade = new OAuth2AuthenticationFacade[F](googleAuth, userRepo)
      oauthLogin = new OAuth2LoginService[F](oauthFacade)

      hasher <- Argon2iPasswordHashingService.build[F]
      basicLogin = new BasicLoginService[F](userRepo, hasher)

      tokenServ = new JwtTokenService[F](config.key, 24.hours)
      authServ =
        AuthenticationServiceImpl(userRepo, tokenServ, basicLogin, oauthLogin)
      authEndpoints = new AuthenticationController[F](authServ).endpoints

      userServ = new UserServiceImpl[F](oauthFacade, userRepo)
      userEndpoints = new UsersController[F](userServ).endpoints

      allEndpoints = authEndpoints ++ userEndpoints
      clientService = AuthenticationService.make(authServ)
    yield new AuthApp[F]:
      override val clientAuthentication: AuthenticationService[F] =
        clientService
      override val endpoints: List[ServerEndpoint[Any, F]] = allEndpoints
  }
