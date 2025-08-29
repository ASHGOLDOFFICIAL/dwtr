package org.aulune
package translations


import shared.UUIDv7Gen.uuidv7Instance
import shared.service.auth.AuthenticationClientService
import shared.service.permission.PermissionClientService
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

import cats.effect.Clock
import cats.effect.kernel.MonadCancelThrow
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.syntax.all.given
import doobie.Transactor
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
  def build[F[_]: MonadCancelThrow: Clock: SecureRandom](
      config: AggregatorConfig,
      authServ: AuthenticationClientService[F],
      permissionServ: PermissionClientService[F],
      transactor: Transactor[F],
  ): F[AggregatorApp[F]] =
    given UUIDGen[F] = uuidv7Instance
    for
      personRepo <- PersonRepositoryImpl.build[F](transactor)
      personServ <- PersonServiceImpl.build[F](personRepo, permissionServ)

      transRepo <- TranslationRepositoryImpl.build[F](transactor)
      transServ <- AudioPlayTranslationServiceImpl
        .build[F](config.pagination, transRepo, permissionServ)

      audioRepo <- AudioPlayRepositoryImpl.build[F](transactor)
      audioServ <- AudioPlayServiceImpl
        .build[F](config.pagination, audioRepo, personServ, permissionServ)

      audioEndpoints = new AudioPlaysController[F](
        config.pagination,
        audioServ,
        authServ,
        transServ).endpoints
      personEndpoints = new PersonsController[F](personServ, authServ).endpoints
      allEndpoints = audioEndpoints ++ personEndpoints
    yield new AggregatorApp[F]:
      override val endpoints: List[ServerEndpoint[Any, F]] = allEndpoints
