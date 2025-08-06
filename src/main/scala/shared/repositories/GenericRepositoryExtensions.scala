package org.aulune
package shared.repositories

import cats.Monad
import cats.syntax.all.*
import org.aulune.shared.errors.RepositoryError


extension [M[_]: Monad, E, Id, Token](repo: GenericRepository[M, E, Id, Token])
  /** Updates element with given ID by applying function.
   *  @param id element ID
   *  @param f function to apply
   *  @return result of operation, element if success, RepositoryError if fail
   */
  def transform(id: Id, f: E => E): M[Either[RepositoryError, E]] =
    repo.get(id).flatMap {
      case Some(e) => repo.update(f(e))
      case None    => RepositoryError.NotFound.asLeft.pure
    }
