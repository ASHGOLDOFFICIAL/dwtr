package org.aulune
package shared.repositories


import shared.errors.RepositoryError
import shared.errors.RepositoryError.NotFound

import cats.Monad
import cats.syntax.all.*


extension [M[_]: Monad, E, Id](repo: GenericRepository[M, E, Id])
  /** Conditionally updates an entity by ID using the provided function.
   *
   *  If the entity is not found, returns [[RepositoryError.NotFound]]. If the
   *  update function results in the same value, the entity is not persisted
   *  again.
   *
   *  @param id ID of the entity.
   *  @param f pure function to transform the existing entity.
   *  @return either a [[RepositoryError]] or the (possibly updated) entity.
   */
  def transform(id: Id, f: E => E): M[Either[RepositoryError, E]] =
    for
      elemOpt <- repo.get(id)
      result  <- elemOpt.fold(NotFound.asLeft.pure[M]) { old =>
        val updated = f(old)
        if updated == old then old.asRight.pure[M]
        else repo.update(updated)
      }
    yield result
