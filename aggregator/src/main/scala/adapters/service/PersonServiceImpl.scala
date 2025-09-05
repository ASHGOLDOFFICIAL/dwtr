package org.aulune.aggregator
package adapters.service


import adapters.service.errors.PersonServiceErrorResponses as ErrorResponses
import adapters.service.mappers.PersonMapper
import application.AggregatorPermission.Modify
import application.dto.person.{
  BatchGetPersonsRequest,
  BatchGetPersonsResponse,
  CreatePersonRequest,
  PersonResource,
}
import application.{AggregatorPermission, PersonService}
import domain.errors.PersonValidationError
import domain.model.person.Person
import domain.repositories.PersonRepository

import cats.MonadThrow
import cats.data.{EitherT, NonEmptyList}
import cats.syntax.all.given
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
   *  @param maxBatchGet maximum allowed elements for batch get request.
   *  @param repo person repository.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   *  @throws IllegalArgumentException when [[maxBatchGet]] is non-positive.
   */
  def build[F[_]: MonadThrow: SortableUUIDGen: LoggerFactory](
      maxBatchGet: Int,
      repo: PersonRepository[F],
      permissionService: PermissionClientService[F],
  ): F[PersonService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    for
      _ <- info"Building service."
      _ <- MonadThrow[F]
        .raiseWhen(maxBatchGet <= 0)(new IllegalArgumentException())
        .onError(_ =>
          error"Non-positive maximum allowed number of get batch request elements.")
      _ <- permissionService.registerPermission(Modify)
    yield new PersonServiceImpl[F](maxBatchGet, repo, permissionService)


private final class PersonServiceImpl[
    F[_]: MonadThrow: SortableUUIDGen: LoggerFactory,
] private (
    maxBatchGet: Int,
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

  override def batchGet(
      request: BatchGetPersonsRequest,
  ): F[Either[ErrorResponse, BatchGetPersonsResponse]] = (for
    _ <- eitherTLogger.info(s"Batch get request: $request.")
    ids <- EitherT.fromEither(parseIdList(request.names))
    elems <- EitherT.liftF(repo.batchGet(ids))
    response <- EitherT.fromEither(checkNoneIsMissing(ids, elems))
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

  /** Transforms list of UUIDs to NEL of typed UUIDs if possible. Or returns the
   *  appropiate error response.
   *  @param ids list of persons IDs.
   */
  private def parseIdList(
      ids: List[UUID],
  ): Either[ErrorResponse, NonEmptyList[Uuid[Person]]] =
    for
      _ <- Either.raiseWhen(ids.size > maxBatchGet)(
        ErrorResponses.maxExceededBatchGet(maxBatchGet))
      idsO = NonEmptyList.fromList(ids.map(Uuid[Person]))
      parsed <- Either.fromOption(idsO, ErrorResponses.emptyBatchGet)
    yield parsed

  /** Checks that for ID a person is given.
   *  @param ids IDs of persons.
   *  @param persons given persons.
   */
  private def checkNoneIsMissing(
      ids: NonEmptyList[Uuid[Person]],
      persons: List[Person],
  ): Either[ErrorResponse, BatchGetPersonsResponse] =
    val set = persons.map(_.id).toSet
    val missing = ids.filterNot(set.contains)
    for
      _ <- NonEmptyList
        .fromList(missing)
        .toLeft(())
        .leftMap(ErrorResponses.personsNotFound)
      response = PersonMapper.toBatchResponse(persons)
    yield response

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
