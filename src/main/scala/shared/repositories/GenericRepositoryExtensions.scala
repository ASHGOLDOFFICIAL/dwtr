package org.aulune
package shared.repositories


import shared.errors.RepositoryError
import shared.errors.RepositoryError.NotFound

import cats.Monad
import cats.syntax.all.*


extension [M[_]: Monad, E, Id](repo: GenericRepository[M, E, Id])
  /** Updates element with given ID by applying function.
   *  @param id element ID
   *  @param f function to apply
   *  @return result of operation, element if success, RepositoryError if fail
   */
  def transform(id: Id, f: E => E): M[Either[RepositoryError, E]] =
    repo.get(id).flatMap {
      _.fold(NotFound.asLeft.pure[M])(el => repo.update(f(el)))
    }
