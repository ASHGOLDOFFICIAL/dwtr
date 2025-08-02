package org.aulune
package infrastructure.memory

import domain.model.{EntityIdentity, RepositoryError}
import domain.service.GenericRepository

import cats.Applicative
import cats.effect.Ref
import cats.syntax.all.*

class GenericRepositoryImpl[F[_]: Applicative, E, ID](
    mapRef: Ref[F, Map[ID, E]]
)(using identity: EntityIdentity[E, ID])
    extends GenericRepository[F, E, ID]:

  extension (elem: E)
    def id: ID =
      summon[EntityIdentity[E, ID]].identity(elem)

  override def contains(id: ID): F[Boolean] =
    mapRef.get.map(_.contains(id))

  override def persist(elem: E): F[Either[RepositoryError, E]] =
    mapRef.modify { currentMap =>
      if currentMap.contains(elem.id)
      then (currentMap, Left(RepositoryError.AlreadyExists))
      else (currentMap.updated(elem.id, elem), Right(elem))
    }

  override def get(id: ID): F[Option[E]] =
    mapRef.get.map(_.get(id))

  override def list(offset: Int, limit: Int): F[List[E]] =
    mapRef.get.map(_.values.slice(offset, offset + limit).toList)

  override def update(elem: E): F[Either[RepositoryError, E]] =
    mapRef.modify { currentMap =>
      if currentMap.contains(elem.id)
      then (currentMap.updated(elem.id, elem), Right(elem))
      else (currentMap, Left(RepositoryError.NotFound))
    }

  override def delete(id: ID): F[Either[RepositoryError, Unit]] =
    mapRef.modify { prev => (prev.removed(id), Right(())) }

end GenericRepositoryImpl
