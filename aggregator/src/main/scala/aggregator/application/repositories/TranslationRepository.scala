package org.aulune
package aggregator.application.repositories

import commons.types.Uuid
import commons.pagination.{CursorDecoder, CursorEncoder}
import commons.repositories.{GenericRepository, PaginatedList}
import aggregator.application.repositories.TranslationRepository.{
  AudioPlayTranslationCursor,
  AudioPlayTranslationIdentity,
}
import aggregator.domain.model.audioplay.{AudioPlay, AudioPlayTranslation}

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import scala.util.Try


/** Repository for [[AudioPlayTranslation]] objects.
 *
 *  @tparam F effect type.
 */
trait TranslationRepository[F[_]]
    extends GenericRepository[
      F,
      AudioPlayTranslation,
      AudioPlayTranslationIdentity]
    with PaginatedList[F, AudioPlayTranslation, AudioPlayTranslationCursor]


object TranslationRepository:
  /** Translation identity.
   *  @param originalId original work ID.
   *  @param id translation ID.
   */
  final case class AudioPlayTranslationIdentity(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
  )

  /** Cursor to resume pagination of translations.
   *  @param originalId ID of original work.
   *  @param id ID of this translation.
   */
  final case class AudioPlayTranslationCursor(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
  )

  given CursorDecoder[AudioPlayTranslationCursor] = token =>
    Try {
      val decoded = Base64.getUrlDecoder.decode(token)
      val raw = new String(decoded, UTF_8)
      val Array(originalStr, idStr) = raw.split('|')
      val originalId = Uuid[AudioPlay](originalStr).get
      val id = Uuid[AudioPlayTranslation](idStr).get
      AudioPlayTranslationCursor(originalId, id)
    }.toOption

  given CursorEncoder[AudioPlayTranslationCursor] = token =>
    val raw = s"${token.originalId}|${token.id}"
    Base64.getUrlEncoder.withoutPadding.encodeToString(raw.getBytes(UTF_8))
