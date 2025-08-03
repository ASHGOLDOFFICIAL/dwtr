package org.aulune

import api.http.AudioPlaysEndpoint
import domain.service.{AuthService, TranslationService}
import infrastructure.memory
import infrastructure.service.*

import cats.effect.*
import cats.syntax.all.*
import doobie.Transactor
import org.http4s.HttpRoutes
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.*
import org.http4s.implicits.*
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object App extends IOApp.Simple:
  given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run: IO[Unit] =
    (for {
      httpClient <- EmberClientBuilder.default[IO].build

      config     = ConfigSource.defaultReference.loadOrThrow[Config]
      transactor = Transactor.fromDriverManager[IO](
        classOf[org.sqlite.JDBC].getName,
        config.sqlite.uri,
        config.sqlite.user,
        config.sqlite.password,
        None
      )

      authService <- AuthService.build[IO](config.app.key).toResource
      given AuthService[IO] = authService

      translationRepo <- memory.TranslationRepository.build[IO].toResource
      translationPermissions = new TranslationPermissionService[IO]
      translationService <- TranslationService
        .build[IO](translationPermissions, translationRepo)
        .toResource
      given TranslationService[IO] = translationService

      audioRepo <- memory.AudioPlayRepository.build[IO].toResource
      audioPermissions = new AudioPlayPermissionService[IO]
      audioService     = new AudioPlayServiceImpl(
        audioPermissions,
        audioRepo,
        new UuidGenImpl[IO]
      )

      audioPlayEndpoints = new AudioPlaysEndpoint[IO](audioService).endpoints

      endpoints     = audioPlayEndpoints
      docsEndpoints = SwaggerInterpreter()
        .fromServerEndpoints[IO](endpoints, config.app.name, config.app.version)

      appRoutes = Http4sServerInterpreter[IO]().toRoutes(endpoints)
      docsRoutes: HttpRoutes[IO] =
        Http4sServerInterpreter[IO]().toRoutes(docsEndpoints)
      allRoutes = appRoutes <+> docsRoutes

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(config.app.host)
        .withPort(config.app.port)
        .withHttpApp(allRoutes.orNotFound)
        .build
    } yield ()).use(_ => IO.never)

end App
