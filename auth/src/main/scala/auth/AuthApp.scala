package org.aulune
package auth

import auth.adapters.jdbc.postgres.UserRepositoryImpl
import auth.adapters.service.oauth2.GoogleOAuth2CodeExchangeService
import auth.adapters.service.{
  Argon2iPasswordHashingService,
  AuthenticationServiceImpl,
  BasicAuthenticationServiceImpl,
  JwtTokenService,
  OAuth2AuthenticationServiceImpl,
  UserServiceImpl,
}
import auth.api.http.{AuthenticationController, UsersController}
import shared.service.auth.AuthenticationClientService

import cats.effect.Async
import cats.effect.std.UUIDGen
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
  val clientAuthentication: AuthenticationClientService[F]
  val endpoints: List[ServerEndpoint[Any, F]]


object AuthApp:
  /** Builds auth app. It's used to hide authentication wiring logic.
   *  @param config application config.
   *  @param transactor transactor for DB.
   *  @tparam F effect type.
   */
  def build[F[_]: Async: UUIDGen](
      config: AuthConfig,
      transactor: Transactor[F],
  ): F[AuthApp[F]] = EmberClientBuilder.default[F].build.use { httpClient =>
    for
      userRepo <- UserRepositoryImpl.build[F](transactor)

      googleCode <-
        GoogleOAuth2CodeExchangeService.build(config.oauth.google, httpClient)
      oauthService =
        new OAuth2AuthenticationServiceImpl[F](googleCode, userRepo)

      hasher <- Argon2iPasswordHashingService.build[F]
      basicLogin = new BasicAuthenticationServiceImpl[F](userRepo, hasher)

      tokenServ = new JwtTokenService[F](config.issuer, config.key, 24.hours)
      service = new AuthenticationServiceImpl(
        userRepo,
        tokenServ,
        tokenServ,
        basicLogin,
        oauthService)
      authEndpoints = new AuthenticationController[F](service).endpoints

      userServ = new UserServiceImpl[F](oauthService, userRepo)
      userEndpoints = new UsersController[F](userServ).endpoints
      allEndpoints = authEndpoints ++ userEndpoints
    yield new AuthApp[F]:
      override val clientAuthentication: AuthenticationClientService[F] =
        AuthenticationServiceAdapter[F](service)
      override val endpoints: List[ServerEndpoint[Any, F]] = allEndpoints
  }
