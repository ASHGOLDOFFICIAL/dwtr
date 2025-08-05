package org.aulune


import api.http.{AudioPlaysEndpoint, LoginEndpoint}
import domain.service.{AuthenticationService, TranslationService}
import infrastructure.jdbc.sqlite
import infrastructure.memory
import infrastructure.service.*

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
      hasher <- PasswordHasher.build[IO]
      given PasswordHasher[IO] = hasher

      userRepo    <- memory.UserRepository.build[IO]
      authService <- AuthenticationService.build[IO](config.app.key, userRepo)
      given AuthenticationService[IO] = authService

      translationRepo <- sqlite.TranslationRepository.build[IO](transactor)
      translationPermissions = new TranslationPermissionService[IO]
      translationService     = new TranslationServiceImpl(
        config.app.pagination,
        translationPermissions,
        translationRepo)
      given TranslationService[IO] = translationService

      audioRepo <- sqlite.AudioPlayRepository.build[IO](transactor)
      audioPermissions = new AudioPlayPermissionService[IO]
      audioService     = new AudioPlayServiceImpl(
        config.app.pagination,
        audioPermissions,
        audioRepo
      )

      audioPlayEndpoints = new AudioPlaysEndpoint[IO](
        config.app.pagination,
        audioService).endpoints
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
