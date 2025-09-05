package org.aulune.aggregator
package adapters.service


import adapters.service.errors.PersonServiceErrorResponses as ErrorResponses
import adapters.service.mappers.PersonMapper
import application.AggregatorPermission.Modify
import application.dto.person.{CreatePersonRequest, PersonResource}
import application.{AggregatorPermission, PersonService}
import domain.errors.PersonValidationError
import domain.model.person.Person

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all.given
import org.aulune.aggregator.domain.repositories.PersonRepository
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.util.UUID


/** [[PersonService]] implementation. */
object PersonServiceImpl:
  /** Builds a service.
   *  @param repo person repository.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadThrow: SortableUUIDGen: LoggerFactory](
      repo: PersonRepository[F],
      permissionService: PermissionClientService[F],
  ): F[PersonService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    for
      _ <- info"Building service."
      _ <- permissionService.registerPermission(Modify)
    yield new PersonServiceImpl[F](repo, permissionService)


private final class PersonServiceImpl[
    F[_]: MonadThrow: SortableUUIDGen: LoggerFactory,
] private (
    repo: PersonRepository[F],
    permissionService: PermissionClientService[F],
) extends PersonService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def findById(id: UUID): F[Either[ErrorResponse, PersonResource]] =
    val uuid = Uuid[Person](id)
    (for
      _ <- eitherTLogger.info(s"Find request: $id.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.personNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $id")
      response = PersonMapper.toResponse(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreatePersonRequest,
  ): F[Either[ErrorResponse, PersonResource]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = SortableUUIDGen.randomTypedUUID[F, Person]
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        id <- EitherT.liftF(uuid)
        person <- EitherT
          .fromEither(makePerson(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(person))
        response = PersonMapper.toResponse(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(user: User, id: UUID): F[Either[ErrorResponse, Unit]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = Uuid[Person](id)
      info"Delete request $id from $user" >> repo.delete(uuid).map(_.asRight)
    }.handleErrorWith(handleInternal)

  /** Makes person from given creation request and assigned ID.
   *  @param request creation request.
   *  @param id ID assigned to this person.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makePerson(request: CreatePersonRequest, id: Uuid[Person]) =
    PersonMapper
      .fromCreateRequest(request, id)
      .leftMap(ErrorResponses.invalidPerson)
      .toEither

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
