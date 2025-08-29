package org.aulune
package aggregator.adapters.service


import commons.errors.ApplicationServiceError.{InvalidArgument, NotFound}
import commons.errors.{ApplicationServiceError, toApplicationError}
import commons.types.Uuid
import commons.repositories.transformF
import commons.service.auth.User
import commons.service.permission.PermissionClientService
import commons.service.permission.PermissionClientService.requirePermissionOrDeny
import aggregator.application.AggregatorPermission.*
import aggregator.application.dto.person.{PersonRequest, PersonResponse}
import aggregator.application.repositories.PersonRepository
import aggregator.application.{PersonService, AggregatorPermission}
import aggregator.domain.errors.PersonValidationError
import aggregator.domain.errors.PersonValidationError.InvalidArguments
import aggregator.domain.model.person.{FullName, Person}

import cats.MonadThrow
import cats.data.{Validated, ValidatedNec}
import cats.effect.std.UUIDGen
import cats.syntax.all.given

import java.util.UUID


/** [[PersonService]] implementation. */
object PersonServiceImpl:
  /** Builds a service.
   *  @param repo person repository.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadThrow: UUIDGen](
      repo: PersonRepository[F],
      permissionService: PermissionClientService[F],
  ): F[PersonService[F]] =
    for _ <- permissionService.registerPermission(Modify)
    yield new PersonServiceImpl[F](repo, permissionService)


private final class PersonServiceImpl[F[_]: MonadThrow: UUIDGen](
    repo: PersonRepository[F],
    permissionService: PermissionClientService[F],
) extends PersonService[F]:
  given PermissionClientService[F] = permissionService

  override def findById(id: UUID): F[Option[PersonResponse]] =
    val uuid = Uuid[Person](id)
    for result <- repo.get(uuid)
    yield result.map(_.toResponse)

  override def create(
      user: User,
      request: PersonRequest,
  ): F[Either[ApplicationServiceError, PersonResponse]] =
    requirePermissionOrDeny(Modify, user) {
      (for
        id <- UUIDGen.randomUUID[F].map(Uuid[Person])
        person <- request
          .toDomain(id)
          .fold(_ => InvalidArgument.raiseError, _.pure[F])
        persisted <- repo.persist(person)
      yield persisted.toResponse).attempt.map(_.leftMap(toApplicationError))
    }

  override def update(
      user: User,
      id: UUID,
      request: PersonRequest,
  ): F[Either[ApplicationServiceError, PersonResponse]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = Uuid[Person](id)
      (for
        updatedOpt <- repo.transformF(uuid) { old =>
          request.update(old) match
            case Validated.Valid(a)   => a.pure
            case Validated.Invalid(e) => InvalidArgument.raiseError
        }
        updated <- updatedOpt match
          case Some(person) => person.pure
          case None         => NotFound.raiseError[F, Person]
        response = updated.toResponse
      yield response).attempt.map(_.leftMap(toApplicationError))
    }

  override def delete(
      user: User,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = Uuid[Person](id)
      for result <- repo.delete(uuid).attempt
      yield result.leftMap(toApplicationError)
    }

  extension (request: PersonRequest)
    /** Updates old domain object with fields from request.
     *  @param old old domain object.
     *  @return updated domain object if valid.
     */
    private def update(
        old: Person,
    ): ValidatedNec[PersonValidationError, Person] =
      FullName(request.name).map(name => Person(id = old.id, name = name)) match
        case Some(value) => value
        case None        => InvalidArguments.invalidNec

    /** Converts request to domain object and verifies it.
     *  @param id ID assigned to this person.
     *  @return created domain object if valid.
     */
    private def toDomain(
        id: UUID,
    ): ValidatedNec[PersonValidationError, Person] = FullName(request.name).map {
      name => Person(id = Uuid[Person](id), name = name)
    } match
      case Some(value) => value
      case None        => InvalidArguments.invalidNec

  extension (domain: Person)
    /** Converts domain object to response object. */
    private def toResponse: PersonResponse =
      PersonResponse(id = domain.id, name = domain.name)
