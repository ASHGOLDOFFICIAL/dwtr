package org.aulune
package translations.infrastructure.service


import auth.domain.model.AuthenticatedUser
import shared.errors.{
  ApplicationServiceError,
  RepositoryError,
  toApplicationError,
}
import shared.pagination.{PaginationParams, TokenDecoder, TokenEncoder}
import shared.repositories.transform
import shared.service.PermissionService
import shared.service.PermissionService.requirePermissionOrDeny
import translations.application.TranslationService
import translations.application.dto.TranslationRequest
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.*
import translations.domain.repositories.TranslationRepository
import translations.infrastructure.service.TranslationServicePermission.*

import cats.Monad
import cats.data.Validated
import cats.effect.Clock
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.syntax.all.*

import java.time.Instant
import java.util.{Base64, UUID}
import scala.util.Try


final class TranslationServiceImpl[F[_]: Monad: Clock: SecureRandom](
    pagination: Config.Pagination,
)(using
    TranslationRepository[F],
    PermissionService[F, TranslationServicePermission],
) extends TranslationService[F]:
  private val repo = summon[TranslationRepository[F]]

  override def getBy(id: TranslationIdentity): F[Option[Translation]] =
    repo.get(id)

  override def getAll(
      originalType: MediumType,
      originalId: MediaResourceId,
      token: Option[String],
      count: Int,
  ): F[Either[ApplicationServiceError, List[Translation]]] =
    PaginationParams(pagination.max)(count, token) match
      case Validated.Invalid(_) =>
        ApplicationServiceError.BadRequest.asLeft.pure
      case Validated.Valid(params) =>
        val token = params.pageToken.map(_.value)
        for list <- repo.list(token, params.pageSize)
        yield list.asRight

  override def create(
      user: AuthenticatedUser,
      tc: TranslationRequest,
      originalType: MediumType,
      originalId: MediaResourceId,
  ): F[Either[ApplicationServiceError, Translation]] =
    requirePermissionOrDeny(Create, user) {
      for
        id <- UUIDGen.randomUUID[F].map(TranslationId(_))
        identity = TranslationIdentity(originalType, originalId, id)
        now <- Clock[F].realTimeInstant
        translation = tc.toDomain(identity, now)
        result <- repo.persist(translation)
      yield result.leftMap(toApplicationError)
    }

  override def update(
      user: AuthenticatedUser,
      id: TranslationIdentity,
      tc: TranslationRequest,
  ): F[Either[ApplicationServiceError, Translation]] =
    requirePermissionOrDeny(Update, user) {
      repo
        .transform(id, old => tc.update(old))
        .map(_.leftMap(toApplicationError))
    }

  override def delete(
      user: AuthenticatedUser,
      id: TranslationIdentity,
  ): F[Either[ApplicationServiceError, Unit]] =
    requirePermissionOrDeny(Delete, user) {
      repo.delete(id).map(_.leftMap(toApplicationError))
    }

  extension (t: TranslationRequest)
    private def update(old: Translation): Translation = old.copy(
      title = TranslationTitle(t.title),
      links = t.links,
    )

    private def toDomain(
        id: TranslationIdentity,
        addedAt: Instant,
    ): Translation = Translation(
      id = id.id,
      title = TranslationTitle(t.title),
      originalType = id.medium,
      originalId = id.parent,
      links = t.links,
      addedAt = addedAt,
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
