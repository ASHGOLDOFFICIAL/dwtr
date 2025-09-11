package org.aulune.commons
package repositories


import cats.Monad
import cats.syntax.all.*


extension [M[_]: Monad, E, Id](repo: GenericRepository[M, E, Id])
  /** Conditionally updates an entity by ID using the provided function.
   *
   *  If the entity is not found, `None` will be returned. If the update
   *  function results in the same value, the entity is not persisted again.
   *
   *  @param id ID of the entity.
   *  @param f pure function to transform the existing entity.
   *  @return updated element if element existed.
   */
  def transform[A](id: Id)(f: E => E): M[Option[E]] =
    for
      elemOpt <- repo.get(id)
      result <- elemOpt.traverse { elem =>
        val updated = f(elem)
        if elem == updated then elem.pure[M]
        else repo.update(updated)
      }
    yield result

  /** Conditionally updates an entity by ID using the provided function.
   *
   *  If the entity is not found, `None` will be returned. If the update
   *  function results in the same value, the entity is not persisted again.
   *
   *  @param f function to be applied to element.
   *  @param id ID of entity to be updated.
   *  @return updated element if element existed.
   */
  def transformF(id: Id)(f: E => M[E]): M[Option[E]] =
    for
      elemOpt <- repo.get(id)
      updatedOpt <- elemOpt.traverse(f)
      pairOpt = elemOpt.zip(updatedOpt)
      result <- pairOpt.traverse { (elem, updated) =>
        if elem == updated then elem.pure[M]
        else repo.update(updated)
      }
    yield result
