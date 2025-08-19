package org.aulune


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
import translations.adapters.jdbc.postgres.{
  AudioPlayRepositoryImpl,
  TranslationRepositoryImpl,
}
import translations.adapters.service.{
  AudioPlayAuthorizationService,
  AudioPlayServiceImpl,
  AudioPlayTranslationServiceImpl,
  TranslationAuthorizationService,
}
import translations.api.http.AudioPlaysController

import cats.effect.{Async, IO, IOApp}
import cats.syntax.all.*
import doobie.Transactor
import org.http4s.HttpRoutes
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import sttp.apispec.openapi.Server
import sttp.apispec.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUI

import scala.concurrent.duration.DurationInt


object App extends IOApp.Simple:
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val config = ConfigSource.defaultReference.loadOrThrow[Config]
  private val transactor = Transactor.fromDriverManager[IO](
    driver = classOf[org.postgresql.Driver].getName,
    url = config.postgres.uri,
    user = config.postgres.user,
    password = config.postgres.password,
    logHandler = None,
  )

  override def run: IO[Unit] = (for
    httpClient <- EmberClientBuilder.default[IO].build
    userRepo <- UserRepositoryImpl.build[IO](transactor).toResource

    googleAuth <- GoogleOAuth2AuthenticationService
      .build(config.oauth.google, httpClient)
      .toResource
    oauthFacade = new OAuth2AuthenticationFacade[IO](googleAuth, userRepo)
    oauthLogin = new OAuth2LoginService[IO](oauthFacade)

    hasher <- Argon2iPasswordHashingService.build[IO].toResource
    basicLogin = new BasicLoginService[IO](userRepo, hasher)

    tokenServ = new JwtTokenService[IO](config.app.key, 24.hours)
    authServ =
      AuthenticationServiceImpl(userRepo, tokenServ, basicLogin, oauthLogin)

    transRepo <- TranslationRepositoryImpl.build[IO](transactor).toResource
    transAuth = new TranslationAuthorizationService[IO]
    transServ = new AudioPlayTranslationServiceImpl[IO](
      config.app.pagination,
      transRepo,
      transAuth)

    audioRepo <- AudioPlayRepositoryImpl.build[IO](transactor).toResource
    audioAuth = new AudioPlayAuthorizationService[IO]
    audioServ =
      new AudioPlayServiceImpl[IO](config.app.pagination, audioRepo, audioAuth)

    userServ = new UserServiceImpl[IO](oauthFacade, userRepo)
    userEndpoints = new UsersController[IO](userServ).endpoints
    authEndpoints = new AuthenticationController[IO](authServ).endpoints

    audioPlayEndpoints = new AudioPlaysController[IO](
      config.app.pagination,
      audioServ,
      authServ,
      transServ).endpoints

    endpoints = authEndpoints ++ userEndpoints ++ audioPlayEndpoints

    _ <- EmberServerBuilder
      .default[IO]
      .withHost(config.app.host)
      .withPort(config.app.port)
      .withHttpApp(makeRoutes(List("v1"), endpoints, config).orNotFound)
      .build
  yield ()).use(_ => IO.never)

  private def makeRoutes[F[_]: Async](
      mountPoint: List[String],
      endpoints: List[ServerEndpoint[Any, F]],
      config: Config,
  ) =
    val appRoutes = Http4sServerInterpreter[F]().toRoutes(endpoints)
    val docsRoutes = makeSwaggerRoutes(mountPoint, endpoints, config)
    Router("/" + mountPoint.mkString("/") -> (appRoutes <+> docsRoutes))

  private def makeSwaggerRoutes[F[_]: Async](
      mountPoint: List[String],
      endpoints: List[ServerEndpoint[Any, F]],
      config: Config,
  ) =
    val openApiYaml = OpenAPIDocsInterpreter()
      .toOpenAPI(
        endpoints.map(_.endpoint),
        title = config.app.name,
        version = config.app.version,
      )
      .addServer(
        Server(
          s"http://localhost:${config.app.port}/${mountPoint.mkString("/")}")
          .description("Local development server"))
      .toYaml
    Http4sServerInterpreter[F]().toRoutes(SwaggerUI[F](openApiYaml))
