package org.aulune
package shared.adapters.repositories.memory


import shared.errors.RepositoryError
import shared.repositories.{EntityIdentity, GenericRepository}

import cats.Applicative
import cats.effect.Ref
import cats.syntax.all.*


/** [[GenericRepository]] in-memory implementation.
 *  @param mapR [[Ref]] with [[Map]]
 *  @tparam F effect type.
 *  @tparam E element type.
 *  @tparam Id element identity type.
 */
class GenericRepositoryImpl[F[_]: Applicative, E, Id](
    mapR: Ref[F, Map[Id, E]],
)(using
    EntityIdentity[E, Id],
) extends GenericRepository[F, E, Id]:
  extension (elem: E)
    private def id: Id = summon[EntityIdentity[E, Id]].identity(elem)

  override def contains(id: Id): F[Boolean] = mapR.get.map(_.contains(id))

  override def persist(elem: E): F[Either[RepositoryError, E]] = mapR.modify {
    currentMap =>
      if currentMap.contains(elem.id)
      then (currentMap, Left(RepositoryError.AlreadyExists))
      else (currentMap.updated(elem.id, elem), Right(elem))
  }

  override def get(id: Id): F[Option[E]] = mapR.get.map(_.get(id))

  override def update(elem: E): F[Either[RepositoryError, E]] = mapR.modify {
    currentMap =>
      if currentMap.contains(elem.id)
      then (currentMap.updated(elem.id, elem), Right(elem))
      else (currentMap, Left(RepositoryError.NotFound))
  }

  override def delete(id: Id): F[Either[RepositoryError, Unit]] =
    mapR.modify(prev => (prev.removed(id), Right(())))
