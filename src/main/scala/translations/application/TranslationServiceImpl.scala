package org.aulune
package translations.application


import auth.domain.model.{AuthenticatedUser, User}
import shared.pagination.{PaginationParams, TokenDecoder, TokenEncoder}
import shared.repositories.{RepositoryError, transform}
import shared.service.PermissionService
import translations.application.TranslationServicePermission.*
import translations.application.dto.TranslationRequest
import translations.application.{
  TranslationService,
  TranslationServiceError,
  TranslationServicePermission
}
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.*
import translations.domain.repositories.TranslationRepository

import cats.data.Validated
import cats.effect.std.UUIDGen
import cats.effect.{Async, Clock, Ref}
import cats.syntax.all.*

import java.time.Instant
import java.util.{Base64, UUID}
import scala.util.Try


class TranslationServiceImpl[F[_]: Async: Clock](
    pagination: Config.Pagination,
    permissionService: PermissionService[F, TranslationServicePermission],
    repo: TranslationRepository[F]
) extends TranslationService[F]:
  override def getBy(id: TranslationIdentity): F[Option[Translation]] =
    repo.get(id)

  override def getAll(
      originalType: MediumType,
      originalId: MediaResourceId,
      token: Option[String],
      count: Int
  ): F[Either[TranslationServiceError, List[Translation]]] =
    PaginationParams(pagination.max)(count, token) match {
      case Validated.Invalid(_) =>
        TranslationServiceError.BadRequest.asLeft.pure
      case Validated.Valid(params) =>
        val token = params.pageToken.map(_.value)
        for list <- repo.list(token, params.pageSize)
        yield list.asRight
    }

  override def create(
      user: AuthenticatedUser,
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceId
  ): F[Either[TranslationServiceError, Translation]] =
    requirePermission(Create, user) {
      for
        id <- UUIDGen.randomUUID[F].map(TranslationId(_))
        identity = TranslationIdentity(originalType, originalId, id)
        now <- Clock[F].realTimeInstant
        translation = tc.toDomain(identity, now)
        result <- repo.persist(translation)
      yield result.leftMap(toTranslationError)
    }

  override def update(
      user: AuthenticatedUser,
      id: TranslationIdentity,
      tc: TranslationRequest
  ): F[Either[TranslationServiceError, Translation]] =
    requirePermission(Update, user) {
      repo
        .transform(id, old => tc.update(old))
        .map(_.leftMap(toTranslationError))
    }

  override def delete(
      user: AuthenticatedUser,
      id: TranslationIdentity
  ): F[Either[TranslationServiceError, Unit]] =
    requirePermission(Delete, user) {
      repo.delete(id).map(_.leftMap(toTranslationError))
    }

  private def toTranslationError(
      err: RepositoryError
  ): TranslationServiceError = err match
    case RepositoryError.AlreadyExists  => TranslationServiceError.AlreadyExists
    case RepositoryError.NotFound       => TranslationServiceError.NotFound
    case RepositoryError.StorageFailure => TranslationServiceError.InternalError

  private def requirePermission[A] =
    PermissionService.requirePermission(permissionService) {
      TranslationServiceError.PermissionDenied.asLeft[A].pure[F]
    }

  extension (t: TranslationRequest)
    private def update(old: Translation): Translation = old.copy(
      title = TranslationTitle(t.title),
      links = t.links
    )

    private def toDomain(
        id: TranslationIdentity,
        addedAt: Instant
    ): Translation = Translation(
      id = id.id,
      title = TranslationTitle(t.title),
      originalType = id.medium,
      originalId = id.parent,
      links = t.links,
      addedAt = addedAt
    )

  // TODO: Make better
  private given TokenDecoder[(TranslationIdentity, Instant)] = token =>
    Try {
      val raw = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
      val Array(mediaStr, parentStr, idStr, timeStr) = raw.split('|')
      val media    = MediumType.fromOrdinal(mediaStr.toInt)
      val parent   = MediaResourceId.unsafeApply(parentStr)
      val id       = TranslationId(UUID.fromString(idStr))
      val instant  = Instant.ofEpochMilli(timeStr.toLong)
      val identity = TranslationIdentity(media, parent, id)
      (identity, instant)
    }.toOption

  private given TokenEncoder[(TranslationIdentity, Instant)] =
    case (identity, instant) =>
      val raw = s"${identity.medium.ordinal}|" +
        s"${identity.parent.string}|" +
        s"${identity.id.string}|" +
        s"${instant.toEpochMilli}"
      Try(
        Base64.getUrlEncoder.withoutPadding.encodeToString(
          raw.getBytes("UTF-8"))).toOption
