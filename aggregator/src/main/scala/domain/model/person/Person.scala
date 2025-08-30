package org.aulune.aggregator
package domain.model.person


import domain.errors.PersonValidationError
import domain.errors.PersonValidationError.*

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*
import org.aulune.commons.types.Uuid


/** Person (primarily a creator of some sort).
 *  @param id person unique ID.
 *  @param name full name.
 */
final case class Person private (
    id: Uuid[Person],
    name: FullName,
)


object Person:
  private type ValidationResult[A] = ValidatedNec[PersonValidationError, A]

  /** Creates a person with state validation.
   *  @param id person unique ID.
   *  @param name full name.
   *  @return person validation result.
   */
  def apply(id: Uuid[Person], name: FullName): ValidationResult[Person] =
    new Person(id, name).validNec

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param id person's unique ID.
   *  @param name full name.
   *  @throws PersonValidationError if given parameters are invalid.
   */
  def unsafe(id: Uuid[Person], name: FullName): Person = Person(id, name) match
    case Validated.Valid(a)   => a
    case Validated.Invalid(e) => throw e.head
