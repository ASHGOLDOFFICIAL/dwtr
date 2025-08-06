package org.aulune
package shared.infrastructure.repositories.memory


import shared.repositories.{EntityIdentity, GenericRepository, RepositoryError}

import cats.Applicative
import cats.effect.Ref
import cats.syntax.all.*


class GenericRepositoryImpl[F[_]: Applicative, E, Id, Token](
    mapRef: Ref[F, Map[Id, E]]
)(using
    EntityIdentity[E, Id]
) extends GenericRepository[F, E, Id, Token]:
  extension (elem: E) private def id: Id = EntityIdentity[E, Id].identity(elem)

  override def contains(id: Id): F[Boolean] = mapRef.get.map(_.contains(id))

  override def persist(elem: E): F[Either[RepositoryError, E]] = mapRef.modify {
    currentMap =>
      if currentMap.contains(elem.id)
      then (currentMap, Left(RepositoryError.AlreadyExists))
      else (currentMap.updated(elem.id, elem), Right(elem))
  }

  override def get(id: Id): F[Option[E]] = mapRef.get.map(_.get(id))

  override def list(startWith: Option[Token], count: Int): F[List[E]] =
    mapRef.get.map { all =>
      val sorted = all.values.toList.sortBy(_.id.toString)
      sorted.indexWhere(a => a == id, 0) match
        case -1 => Nil
        case x  => sorted.slice(x, x + count)
    }

  override def update(elem: E): F[Either[RepositoryError, E]] = mapRef.modify {
    currentMap =>
      if currentMap.contains(elem.id)
      then (currentMap.updated(elem.id, elem), Right(elem))
      else (currentMap, Left(RepositoryError.NotFound))
  }

  override def delete(id: Id): F[Either[RepositoryError, Unit]] =
    mapRef.modify(prev => (prev.removed(id), Right(())))
