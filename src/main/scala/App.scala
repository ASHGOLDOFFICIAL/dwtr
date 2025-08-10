package org.aulune


import auth.adapters.memory.UserRepositoryImpl
import auth.adapters.service.{
  Argon2iPasswordHashingService,
  AuthenticationServiceImpl,
}
import auth.api.http.LoginController
import translations.adapters.jdbc.sqlite.{
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

  override def run: IO[Unit] =
    val config = ConfigSource.defaultReference.loadOrThrow[Config]
    val transactor = Transactor.fromDriverManager[IO](
      driver = classOf[org.sqlite.JDBC].getName,
      url = config.sqlite.uri,
      user = config.sqlite.user,
      password = config.sqlite.password,
      logHandler = None,
    )

    for
      hasher <- Argon2iPasswordHashingService.build[IO]
      userRepo <- UserRepositoryImpl.build[IO]
      authServ =
        AuthenticationServiceImpl(config.app.key, 24.hours, userRepo, hasher)

      transRepo <- TranslationRepositoryImpl.build[IO](transactor)
      transAuth = new TranslationAuthorizationService[IO]
      transServ = new AudioPlayTranslationServiceImpl[IO](
        config.app.pagination,
        transRepo,
        transAuth)

      audioRepo <- AudioPlayRepositoryImpl.build[IO](transactor)
      audioAuth = new AudioPlayAuthorizationService[IO]
      audioServ = new AudioPlayServiceImpl[IO](
        config.app.pagination,
        audioRepo,
        audioAuth)

      audioPlayEndpoints = new AudioPlaysController[IO](
        config.app.pagination,
        audioServ,
        authServ,
        transServ).endpoints
      endpoints =
        audioPlayEndpoints ++ new LoginController[IO](authServ).endpoints

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
      .addServer(Server(
        s"http://localhost:${config.app.port.value}/${mountPoint.mkString("/")}")
        .description("Local development server"))
      .toYaml
    Http4sServerInterpreter[F]().toRoutes(SwaggerUI[F](openApiYaml))
