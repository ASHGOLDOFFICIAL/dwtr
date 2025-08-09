package org.aulune
package translations.application.repositories


import shared.pagination.{TokenDecoder, TokenEncoder}
import shared.repositories.{GenericRepository, PaginatedList}
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.domain.model.audioplay.AudioPlay
import translations.domain.shared.Uuid

import java.time.Instant
import java.util.Base64
import scala.util.Try


/** Repository for [[AudioPlay]] objects.
 *  @tparam F effect type.
 */
trait AudioPlayRepository[F[_]]
    extends GenericRepository[F, AudioPlay, Uuid[AudioPlay]]
    with PaginatedList[F, AudioPlay, AudioPlayToken]


object AudioPlayRepository:
  /** Token to identify pagination params.
   *  @param identity identity of [[AudioPlay]].
   *  @param timestamp when audio play was added.
   */
  final case class AudioPlayToken(
      identity: Uuid[AudioPlay],
      timestamp: Instant,
  )

  // TODO: Make better
  given TokenDecoder[AudioPlayToken] = token =>
    Try {
      val raw = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
      val Array(idStr, timeStr) = raw.split('|')
      val id                    = Uuid[AudioPlay](idStr).get
      val instant               = Instant.ofEpochMilli(timeStr.toLong)
      AudioPlayToken(id, instant)
    }.toOption

  given TokenEncoder[AudioPlayToken] = token =>
    val raw = s"${token.identity.toString}|" +
      s"${token.timestamp.toEpochMilli}"
    Try(
      Base64.getUrlEncoder.withoutPadding.encodeToString(
        raw.getBytes("UTF-8"))).toOption
