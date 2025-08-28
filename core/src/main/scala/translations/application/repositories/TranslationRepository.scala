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
   *
   *  @param identity identity of [[AudioPlayTranslation]].
   *  @param timestamp when translation was added.
   */
  final case class AudioPlayTranslationToken(
      identity: AudioPlayTranslationIdentity,
      timestamp: Instant,
  )

  // TODO: Make better
  given TokenDecoder[AudioPlayTranslationToken] = token =>
    Try {
      val raw = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
      val Array(originalStr, idStr, timeStr) = raw.split('|')
      val orig = Uuid[AudioPlay](originalStr).get
      val id = Uuid[AudioPlayTranslation](idStr).get
      val instant = Instant.ofEpochMilli(timeStr.toLong)
      val identity = AudioPlayTranslationIdentity(orig, id)
      AudioPlayTranslationToken(identity, instant)
    }.toOption

  given TokenEncoder[AudioPlayTranslationToken] = token =>
    val raw = s"${token.identity.originalId}|" +
      s"${token.identity.id}|" +
      s"${token.timestamp.toEpochMilli}"
    Try(
      Base64.getUrlEncoder.withoutPadding.encodeToString(
        raw.getBytes("UTF-8"))).toOption
