package org.aulune


import aggregator.AggregatorApp
import auth.AuthApp
import permissions.PermissionApp

import cats.effect.kernel.Resource
import cats.effect.{Async, IO, IOApp}
import cats.mtl.Handle.handleForApplicativeError
import cats.syntax.all.given
import doobie.Transactor
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.{HttpRoutes, server}
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import sttp.apispec.openapi.Server
import sttp.apispec.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUI


object App extends IOApp.Simple:
  private given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

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
      permissionApp <- PermissionApp.build(config.permissions, transactor)
      aggregatorApp <- AggregatorApp.build(
        config.aggregator,
        authApp.clientAuthentication,
        permissionApp.clientService,
        transactor)
      endpoints = authApp.endpoints ++ aggregatorApp.endpoints
      _ <- makeServer[IO](endpoints).use(_ => IO.never)
    yield ()

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
