package org.aulune
package translations.adapters.service


import auth.application.dto.AuthenticatedUser
import shared.errors.ApplicationServiceError.{BadRequest, NotFound}
import shared.errors.{ApplicationServiceError, toApplicationError}
import shared.model.Uuid
import shared.repositories.transformFP
import shared.service.AuthorizationService
import shared.service.AuthorizationService.requirePermissionOrDeny
import translations.application.AudioPlayPermission.*
import translations.application.dto.person.{PersonRequest, PersonResponse}
import translations.application.repositories.PersonRepository
import translations.application.{AudioPlayPermission, PersonService}
import translations.domain.errors.PersonValidationError
import translations.domain.model.person.{FullName, Person}

import cats.MonadThrow
import cats.data.{Validated, ValidatedNec}
import cats.effect.std.UUIDGen
import cats.syntax.all.given

import java.util.UUID


/** [[PersonService]] implementation.
 *  @param repo person repository.
 *  @param authService [[AuthorizationService]] for [[AudioPlayPermission]]s.
 *  @tparam F effect type.
 */
final class PersonServiceImpl[F[_]: MonadThrow: UUIDGen](
    repo: PersonRepository[F],
    authService: AuthorizationService[F, AudioPlayPermission],
) extends PersonService[F]:
  given AuthorizationService[F, AudioPlayPermission] = authService

  override def findById(id: UUID): F[Option[PersonResponse]] =
    val uuid = Uuid[Person](id)
    for result <- repo.get(uuid)
    yield result.map(_.toResponse)

  override def create(
      user: AuthenticatedUser,
      request: PersonRequest,
  ): F[Either[ApplicationServiceError, PersonResponse]] =
    requirePermissionOrDeny(Write, user) {
      (for
        id <- UUIDGen.randomUUID[F].map(Uuid[Person])
        person <- request
          .toDomain(id)
          .fold(_ => BadRequest.raiseError, _.pure[F])
        persisted <- repo.persist(person)
      yield persisted.toResponse).attempt.map(_.leftMap(toApplicationError))
    }

  override def update(
      user: AuthenticatedUser,
      id: UUID,
      request: PersonRequest,
  ): F[Either[ApplicationServiceError, PersonResponse]] =
    requirePermissionOrDeny(Write, user) {
      val uuid = Uuid[Person](id)
      (for
        updatedOpt <- repo.transformFP(uuid) { old =>
          request.update(old) match
            case Validated.Valid(a)   => a.pure
            case Validated.Invalid(e) => BadRequest.raiseError
        }
        updated <- updatedOpt match
          case Some(person) => person.pure
          case None         => NotFound.raiseError[F, Person]
        response = updated.toResponse
      yield response).attempt.map(_.leftMap(toApplicationError))
    }

  override def delete(
      user: AuthenticatedUser,
      id: UUID,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Write, user) {
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
        case None        => PersonValidationError.InvalidArguments.invalidNec

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
      case None        => PersonValidationError.InvalidArguments.invalidNec

  extension (domain: Person)
    /** Converts domain object to response object. */
    private def toResponse: PersonResponse =
      PersonResponse(id = domain.id, name = domain.name)
