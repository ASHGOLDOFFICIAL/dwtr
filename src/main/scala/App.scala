package org.aulune


import auth.api.http.LoginEndpoint
import auth.application.AuthenticationService
import auth.domain.repositories.UserRepository
import auth.domain.service.PasswordHashingService
import auth.infrastructure.memory.UserRepositoryImpl
import auth.infrastructure.service.{
  Argon2iPasswordHashingService,
  TokenAuthenticationService,
}
import shared.service.PermissionService
import translations.api.http.AudioPlaysController
import translations.application.*
import translations.domain.repositories.{
  AudioPlayRepository,
  TranslationRepository,
}
import translations.infrastructure.jdbc.sqlite.{
  AudioPlayRepositoryImpl,
  TranslationRepositoryImpl,
}
import translations.infrastructure.service.*

import cats.effect.*
import cats.syntax.all.*
import doobie.Transactor
import org.http4s.HttpRoutes
import org.http4s.ember.server.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import sttp.apispec.openapi.Server
import sttp.apispec.openapi.circe.yaml.*
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUI


object App extends IOApp.Simple:
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run: IO[Unit] =
    val config     = ConfigSource.defaultReference.loadOrThrow[Config]
    val transactor = Transactor.fromDriverManager[IO](
      driver = classOf[org.sqlite.JDBC].getName,
      url = config.sqlite.uri,
      user = config.sqlite.user,
      password = config.sqlite.password,
      logHandler = None,
    )

    for
      given PasswordHashingService[IO] <-
        Argon2iPasswordHashingService.build[IO]

      given UserRepository[IO]        <- UserRepositoryImpl.build[IO]
      given AuthenticationService[IO] <-
        TokenAuthenticationService.build[IO](config.app.key)

      given PermissionService[IO, TranslationServicePermission] =
        new TranslationPermissionService[IO]

      given TranslationRepository[IO] <-
        TranslationRepositoryImpl.build[IO](transactor)
      given TranslationService[IO] =
        new TranslationServiceImpl[IO](config.app.pagination)

      given AudioPlayRepository[IO] <-
        AudioPlayRepositoryImpl.build[IO](transactor)
      given PermissionService[IO, AudioPlayServicePermission] =
        new AudioPlayPermissionService[IO]
      given AudioPlayService[IO] =
        new AudioPlayServiceImpl[IO](config.app.pagination)

      audioPlayEndpoints =
        new AudioPlaysController[IO](config.app.pagination).endpoints
      endpoints = audioPlayEndpoints :+ new LoginEndpoint[IO].loginEndpoint

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(config.app.host)
        .withPort(config.app.port)
        .withHttpApp(makeRoutes(List("v1"), endpoints, config).orNotFound)
        .build
        .use(_ => IO.never)
    yield ()

  private def makeRoutes[F[_]: Async](
      mountPoint: List[String],
      endpoints: List[ServerEndpoint[Any, F]],
      config: Config,
  ) =
    val appRoutes  = Http4sServerInterpreter[F]().toRoutes(endpoints)
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
      .addServer(Server(
        s"http://localhost:${config.app.port.value}/${mountPoint.mkString("/")}")
        .description("Local development server"))
      .toYaml
    Http4sServerInterpreter[F]().toRoutes(SwaggerUI[F](openApiYaml))
