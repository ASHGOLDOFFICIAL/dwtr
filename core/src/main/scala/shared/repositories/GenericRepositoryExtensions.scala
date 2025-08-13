package org.aulune
package shared.repositories


import shared.errors.RepositoryError
import shared.errors.RepositoryError.NotFound

import cats.Monad
import cats.data.EitherT
import cats.syntax.all.*

import scala.util.control.NoStackTrace


object InvalidInput extends NoStackTrace


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

  /** Updates element to result of [[f]]. If [[f]] returns `None`, then [[err]]
   *  wrapped in `Left` will be returned.
   *
   *  If function [[f]] results in the same value, the entity is not persisted.
   *
   *  @param id ID of entity to be updated.
   *  @param err error to be returned if [[f]] returns `None`
   *  @param f function to be applied to element.
   *  @param g function to transform errors.
   *  @tparam B new error type.
   *  @return updated element or error.
   */
  def transformIfSome[B](id: Id, err: B)(
      f: E => Option[E],
  )(g: RepositoryError => B): M[Either[B, E]] = (for
    elem    <- EitherT.fromOptionF(repo.get(id), g(RepositoryError.NotFound))
    updated <- EitherT.fromOption(f(elem), err)
    result  <- EitherT {
      if updated == elem then elem.asRight.pure[M]
      else repo.update(updated)
    }.leftMap(g)
  yield result).value
