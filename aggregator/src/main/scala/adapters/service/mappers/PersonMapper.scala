package org.aulune.aggregator
package adapters.service.mappers


import application.dto.person.{PersonRequest, PersonResponse}
import domain.errors.PersonValidationError
import domain.errors.PersonValidationError.InvalidArguments
import domain.model.person.{FullName, Person}

import cats.data.ValidatedNec
import cats.syntax.all.given
import org.aulune.commons.types.Uuid


/** Mapper between person DTOs and domain [[Person]]. */
object PersonMapper:
  /** Converts request to domain object and verifies it.
   *  @param request person creation request.
   *  @param id ID assigned to this person.
   *  @return created domain object if valid.
   */
  def fromCreateRequest(
      request: PersonRequest,
      id: Uuid[Person],
  ): ValidatedNec[PersonValidationError, Person] =
    FullName(request.name).map(name => Person(id = id, name = name)) match
      case Some(person) => person
      case None         => InvalidArguments.invalidNec

  /** Converts domain object to response object. */
  def toResponse(domain: Person): PersonResponse =
    PersonResponse(id = domain.id, name = domain.name)
