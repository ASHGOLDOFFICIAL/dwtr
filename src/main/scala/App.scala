package org.aulune


import auth.api.http.LoginEndpoint
import auth.application.AuthenticationService
import auth.domain.repositories.UserRepository
import auth.domain.service.PasswordHashingService
import auth.infrastructure.memory.UserRepository
import auth.infrastructure.service.{
  Argon2iPasswordHashingService,
  AuthenticationServiceImpl
}
import shared.service.PermissionService
import translations.api.http.AudioPlaysEndpoint
import translations.application.*
import translations.domain.repositories.{
  AudioPlayRepository,
  TranslationRepository
}
import translations.infrastructure.jdbc.sqlite.{
  AudioPlayRepository,
  TranslationRepository
}

import cats.effect.*
import cats.syntax.all.*
import doobie.Transactor
import org.http4s.HttpRoutes
import org.http4s.ember.server.*
import org.http4s.implicits.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter


object App extends IOApp.Simple:
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run: IO[Unit] =
    val config     = ConfigSource.defaultReference.loadOrThrow[Config]
    val transactor = Transactor.fromDriverManager[IO](
      classOf[org.sqlite.JDBC].getName,
      config.sqlite.uri,
      config.sqlite.user,
      config.sqlite.password,
      None
    )

    for
      given PasswordHashingService[IO] <-
        Argon2iPasswordHashingService.build[IO]

      given UserRepository[IO]        <- UserRepository.build[IO]
      given AuthenticationService[IO] <-
        AuthenticationServiceImpl.build[IO](config.app.key)

      given PermissionService[IO, TranslationServicePermission] =
        new TranslationPermissionService[IO]

      given TranslationRepository[IO] <-
        TranslationRepository.build[IO](transactor)
      given TranslationService[IO] =
        new TranslationServiceImpl[IO](config.app.pagination)

      given AudioPlayRepository[IO] <- AudioPlayRepository.build[IO](transactor)
      given PermissionService[IO, AudioPlayServicePermission] =
        new AudioPlayPermissionService[IO]
      given AudioPlayService[IO] =
        new AudioPlayServiceImpl[IO](config.app.pagination)

      audioPlayEndpoints =
        new AudioPlaysEndpoint[IO](config.app.pagination).endpoints
      endpoints = audioPlayEndpoints :+ new LoginEndpoint[IO].loginEndpoint

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(config.app.host)
        .withPort(config.app.port)
        .withHttpApp(routes(config, endpoints).orNotFound)
        .build
        .use(_ => IO.never)
    yield ()

  private def routes(
      config: Config,
      endpoints: List[ServerEndpoint[Any, IO]]
  ) =
    val docsEndpoints = SwaggerInterpreter()
      .fromServerEndpoints[IO](endpoints, config.app.name, config.app.version)

    val appRoutes = Http4sServerInterpreter[IO]().toRoutes(endpoints)
    val docsRoutes: HttpRoutes[IO] =
      Http4sServerInterpreter[IO]().toRoutes(docsEndpoints)

    appRoutes <+> docsRoutes
