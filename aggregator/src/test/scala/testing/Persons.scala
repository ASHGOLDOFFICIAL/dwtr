package org.aulune.aggregator
package testing


import adapters.service.mappers.PersonMapper
import application.PersonService
import application.dto.person.{CreatePersonRequest, PersonResource}
import domain.model.person.{FullName, Person}

import cats.Applicative
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid

import java.util.UUID


/** [[Person]] objects for testing. */
private[aggregator] object Persons:
  val person1: Person = Person.unsafe(
    id = Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950481"),
    name = FullName.unsafe("John Smith"),
  )

  val person2: Person = Person.unsafe(
    id = Uuid.unsafe("03205f95-7e75-4fb4-b2d9-23549b950482"),
    name = FullName.unsafe("Jane Smith"),
  )

  val person3: Person = Person.unsafe(
    id = Uuid.unsafe("adfeccac-0c8e-4a6c-a0b3-08684e6bd336"),
    name = FullName.unsafe("Peter Jones"),
  )

  /** Stub [[PersonService]] implementation that supports only `findById`
   *  operation.
   *
   *  Contains only persons given in [[Persons]] object.
   *
   *  @tparam F effect type.
   */
  def service[F[_]: Applicative]: PersonService[F] = new PersonService[F]:
    private val personById: Map[Uuid[Person], Person] = Map.from(
      List(person1, person2, person3).map(p => (p.id, p)),
    )

    override def findById(id: UUID): F[Either[ErrorResponse, PersonResource]] =
      personById
        .get(Uuid[Person](id))
        .map(PersonMapper.toResponse)
        .toRight(
          adapters.service.errors.PersonServiceErrorResponses.personNotFound)
        .pure[F]

    override def create(
        user: User,
        pr: CreatePersonRequest,
    ): F[Either[ErrorResponse, PersonResource]] =
      throw new UnsupportedOperationException()

    override def delete(user: User, id: UUID): F[Either[ErrorResponse, Unit]] =
      throw new UnsupportedOperationException()
