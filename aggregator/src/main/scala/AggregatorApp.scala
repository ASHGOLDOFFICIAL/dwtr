package org.aulune.aggregator


import adapters.jdbc.postgres.{
  AudioPlayRepositoryImpl,
  AudioPlayTranslationRepositoryImpl,
  PersonRepositoryImpl
}
import adapters.service.{
  AudioPlayServiceImpl,
  AudioPlayTranslationServiceImpl,
  PersonServiceImpl,
}
import api.http.{
  AudioPlayTranslationsController,
  AudioPlaysController,
  PersonsController,
}

import cats.effect.Clock
import cats.effect.kernel.MonadCancelThrow
import cats.effect.std.SecureRandom
import cats.syntax.all.given
import doobie.Transactor
import org.aulune.commons.instances.UUIDv7Gen.uuidv7Instance
import org.aulune.commons.service.auth.AuthenticationClientService
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.typelevel.log4cats.LoggerFactory
import sttp.tapir.server.ServerEndpoint


/** Aggregator app with Tapir endpoints.
 *  @tparam F effect type.
 */
trait AggregatorApp[F[_]]:
  val endpoints: List[ServerEndpoint[Any, F]]


object AggregatorApp:
  /** Builds an aggregator app.
   *  @param config aggregator app config.
   *  @param transactor transactor for DB.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow: Clock: SecureRandom: LoggerFactory](
      config: AggregatorConfig,
      authServ: AuthenticationClientService[F],
      permissionServ: PermissionClientService[F],
      transactor: Transactor[F],
  ): F[AggregatorApp[F]] =
    given SortableUUIDGen[F] = uuidv7Instance
    for
      personRepo <- PersonRepositoryImpl.build[F](transactor)
      personServ <- PersonServiceImpl.build[F](personRepo, permissionServ)
      personEndpoints = new PersonsController[F](personServ, authServ).endpoints

      audioRepo <- AudioPlayRepositoryImpl.build[F](transactor)
      audioServ <- AudioPlayServiceImpl
        .build[F](
          config.pagination,
          config.search,
          audioRepo,
          personServ,
          permissionServ)
      audioEndpoints = new AudioPlaysController[F](
        config.pagination,
        audioServ,
        authServ).endpoints

      transRepo <- AudioPlayTranslationRepositoryImpl.build[F](transactor)
      transServ <- AudioPlayTranslationServiceImpl
        .build[F](config.pagination, transRepo, audioServ, permissionServ)
      audioTranslationEndpoints = new AudioPlayTranslationsController[F](
        config.pagination,
        transServ,
        authServ,
      ).endpoints

      allEndpoints =
        audioEndpoints ++ audioTranslationEndpoints ++ personEndpoints
    yield new AggregatorApp[F]:
      override val endpoints: List[ServerEndpoint[Any, F]] = allEndpoints
