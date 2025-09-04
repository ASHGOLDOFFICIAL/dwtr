package org.aulune.aggregator
package application.repositories


import application.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor
import domain.model.audioplay.{AudioPlay, AudioPlayTranslation}

import org.aulune.commons.pagination.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{GenericRepository, PaginatedList}
import org.aulune.commons.types.Uuid

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import scala.util.Try


/** Repository for [[AudioPlayTranslation]] objects.
 *
 *  @tparam F effect type.
 */
trait AudioPlayTranslationRepository[F[_]]
    extends GenericRepository[
      F,
      AudioPlayTranslation,
      Uuid[AudioPlayTranslation]]
    with PaginatedList[F, AudioPlayTranslation, AudioPlayTranslationCursor]


object AudioPlayTranslationRepository:
  /** Cursor to resume pagination of translations.
   *  @param id ID of this translation.
   */
  final case class AudioPlayTranslationCursor(
      id: Uuid[AudioPlayTranslation],
  )

  given CursorDecoder[AudioPlayTranslationCursor] = token =>
    Try {
      val decoded = Base64.getUrlDecoder.decode(token)
      val rawId = new String(decoded, UTF_8)
      val id = Uuid[AudioPlayTranslation](rawId).get
      AudioPlayTranslationCursor(id)
    }.toOption

  given CursorEncoder[AudioPlayTranslationCursor] = token =>
    val raw = token.id.toString
    Base64.getUrlEncoder.withoutPadding.encodeToString(raw.getBytes(UTF_8))
