package org.aulune
package translations.application.repositories


import shared.pagination.{TokenDecoder, TokenEncoder}
import shared.repositories.{GenericRepository, PaginatedList}
import translations.application.repositories.TranslationRepository.{
  TranslationIdentity,
  TranslationToken,
}
import translations.domain.model.audioplay.AudioPlay
import translations.domain.model.shared.Uuid
import translations.domain.model.translation.AudioPlayTranslation

import java.time.Instant
import java.util.Base64
import scala.util.Try


/** Repository for [[AudioPlayTranslation]] objects.
 *
 *  @tparam F effect type.
 */
trait TranslationRepository[F[_]]
    extends GenericRepository[F, AudioPlayTranslation, TranslationIdentity]
    with PaginatedList[F, AudioPlayTranslation, TranslationToken]


object TranslationRepository:
  /** Translation identity.
   *  @param originalId original work ID.
   *  @param id translation ID.
   */
  final case class TranslationIdentity(
      originalId: Uuid[AudioPlay],
      id: Uuid[AudioPlayTranslation],
  )

  /** Token to identify pagination params.
   *
   *  @param identity identity of [[AudioPlayTranslation]].
   *  @param timestamp when translation was added.
   */
  final case class TranslationToken(
      identity: TranslationIdentity,
      timestamp: Instant,
  )

  // TODO: Make better
  given TokenDecoder[TranslationToken] = token =>
    Try {
      val raw = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
      val Array(originalStr, idStr, timeStr) = raw.split('|')
      val orig                               = Uuid[AudioPlay](originalStr).get
      val id       = Uuid[AudioPlayTranslation](idStr).get
      val instant  = Instant.ofEpochMilli(timeStr.toLong)
      val identity = TranslationIdentity(orig, id)
      TranslationToken(identity, instant)
    }.toOption

  given TokenEncoder[TranslationToken] = token =>
    val raw = s"${token.identity.originalId}|" +
      s"${token.identity.id}|" +
      s"${token.timestamp.toEpochMilli}"
    Try(
      Base64.getUrlEncoder.withoutPadding.encodeToString(
        raw.getBytes("UTF-8"))).toOption
