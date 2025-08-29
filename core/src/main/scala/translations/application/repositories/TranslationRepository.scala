package org.aulune
package translations.application.repositories


import shared.model.Uuid
import shared.pagination.{TokenDecoder, TokenEncoder}
import shared.repositories.{GenericRepository, PaginatedList}
import translations.application.repositories.TranslationRepository.{
  AudioPlayTranslationIdentity,
  AudioPlayTranslationToken,
}
import translations.domain.model.audioplay.{AudioPlay, AudioPlayTranslation}

import java.time.Instant
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
    with PaginatedList[F, AudioPlayTranslation, AudioPlayTranslationToken]


object TranslationRepository:
  /** Translation identity.
   *  @param originalId original work ID.
   *  @param id translation ID.
   */
  final case class AudioPlayTranslationIdentity(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
  )

  /** Token to identify pagination params.
   *  @param originalId ID of original work.
   *  @param id ID of this translation.
   */
  final case class AudioPlayTranslationToken(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
  )

  // TODO: Make better
  given TokenDecoder[AudioPlayTranslationToken] = token =>
    Try {
      val raw = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
      val Array(originalStr, idStr) = raw.split('|')
      val originalId = Uuid[AudioPlay](originalStr).get
      val id = Uuid[AudioPlayTranslation](idStr).get
      AudioPlayTranslationToken(originalId, id)
    }.toOption

  given TokenEncoder[AudioPlayTranslationToken] = token =>
    val raw = s"${token.originalId}|${token.id}"
    Try(
      Base64.getUrlEncoder.withoutPadding.encodeToString(
        raw.getBytes("UTF-8"))).toOption
