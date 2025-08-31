package org.aulune.aggregator
package adapters.service


import adapters.service.errors.PersonServiceErrorResponses as ErrorResponses
import adapters.service.mappers.PersonMapper
import application.AggregatorPermission.Modify
import application.dto.person.{PersonRequest, PersonResponse}
import application.repositories.PersonRepository
import application.{AggregatorPermission, PersonService}
import domain.errors.PersonValidationError
import domain.errors.PersonValidationError.InvalidArguments
import domain.model.person.{FullName, Person}

import cats.MonadThrow
import cats.data.{EitherT, Validated, ValidatedNec}
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorStatus.{
  InvalidArgument,
  NotFound,
}
import org.aulune.commons.errors.{
  ErrorStatus,
  ErrorResponse,
  toApplicationError,
}
import org.aulune.commons.repositories.transformF
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.types.Uuid

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

  override def findById(id: UUID): F[Either[ErrorResponse, PersonResponse]] =
    val uuid = Uuid[Person](id)
    val getResult = repo.get(uuid).attempt
    (for
      elemOpt <- EitherT(getResult).leftMap(_ => ErrorResponses.internal)
      elem <- EitherT.fromOption(elemOpt, ErrorResponses.personNotFound)
      response = PersonMapper.toResponse(elem)
    yield response).value

  override def create(
      user: User,
      request: PersonRequest,
  ): F[Either[ErrorResponse, PersonResponse]] =
    requirePermissionOrDeny(Modify, user) {
      (for
        id <- EitherT.liftF(UUIDGen.randomUUID[F].map(Uuid[Person]))
        person <- EitherT.fromEither(
          PersonMapper
            .fromCreateRequest(request, id)
            .leftMap(ErrorResponses.invalidPerson)
            .toEither)
        persisted <- repo
          .persist(person)
          .attemptT
          .leftMap(_ => ErrorResponses.internal)
        response = PersonMapper.toResponse(persisted)
      yield response).value
    }

  override def delete(
      user: User,
      id: UUID,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val uuid = Uuid[Person](id)
    for result <- repo.delete(uuid).attempt
    yield result.leftMap(_ => ErrorResponses.internal)
  }
