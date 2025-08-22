package org.aulune
package translations.domain.shared


import translations.domain.errors.PersonValidationError
import translations.domain.errors.PersonValidationError.*

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.*

import java.util.UUID


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
  def apply(id: UUID, name: String): ValidationResult[Person] = (
    Uuid[Person](id).validNec,
    FullName(name).toValidNec(InvalidFullName),
  ).mapN(new Person(_, _))
