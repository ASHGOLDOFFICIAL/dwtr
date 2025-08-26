package org.aulune


import auth.AuthApp
import permissions.PermissionsApp
import shared.auth.AuthenticationClientService
import shared.permission.PermissionClientService
import translations.adapters.jdbc.postgres.{
  AudioPlayRepositoryImpl,
  PersonRepositoryImpl,
  TranslationRepositoryImpl,
}
import translations.adapters.service.{
  AudioPlayServiceImpl,
  AudioPlayTranslationServiceImpl,
  PersonServiceImpl,
}
import translations.api.http.{AudioPlaysController, PersonsController}

import cats.effect.kernel.Resource
import cats.effect.std.SecureRandom
import cats.effect.{Async, Clock, IO, IOApp, MonadCancelThrow}
import cats.syntax.all.*
import doobie.Transactor
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.{HttpRoutes, server}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import sttp.apispec.openapi.Server
import sttp.apispec.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUI


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

  override def run: IO[Unit] =
    for
      authApp <- AuthApp.build(config.auth, transactor)
      permissionApp <- PermissionsApp.build(transactor)
      translationsEndpoints <- makeTranslationsEndpoints[IO](
        authApp.clientAuthentication,
        permissionApp.clientPermission,
        transactor)
      endpoints = authApp.endpoints ++ translationsEndpoints
      _ <- makeServer[IO](endpoints).use(_ => IO.never)
    yield ()

  private def makeTranslationsEndpoints[F[
      _,
  ]: MonadCancelThrow: Clock: SecureRandom](
      authServ: AuthenticationClientService[F],
      permissionServ: PermissionClientService[F],
      transactor: Transactor[F],
  ): F[List[ServerEndpoint[Any, F]]] =
    for
      personRepo <- PersonRepositoryImpl.build[F](transactor)
      personServ = new PersonServiceImpl[F](personRepo, permissionServ)

      transRepo <- TranslationRepositoryImpl.build[F](transactor)
      transServ = new AudioPlayTranslationServiceImpl[F](
        config.app.pagination,
        transRepo,
        permissionServ)

      audioRepo <- AudioPlayRepositoryImpl.build[F](transactor)
      audioServ = new AudioPlayServiceImpl[F](
        config.app.pagination,
        audioRepo,
        personServ,
        permissionServ)

      audioEndpoints = new AudioPlaysController[F](
        config.app.pagination,
        audioServ,
        authServ,
        transServ).endpoints
      personEndpoints = new PersonsController[F](personServ, authServ).endpoints
    yield audioEndpoints ++ personEndpoints

  private def makeServer[F[_]: Async](
      endpoints: List[ServerEndpoint[Any, F]],
  ): Resource[F, server.Server] = EmberServerBuilder
    .default[F]
    .withHost(config.app.host)
    .withPort(config.app.port)
    .withHttpApp(makeRoutes(List("v1"), endpoints, config).orNotFound)
    .build

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
