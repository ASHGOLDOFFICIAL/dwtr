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
   */
  final case class AudioPlayToken(identity: Uuid[AudioPlay])

  // TODO: Make better
  given TokenDecoder[AudioPlayToken] = token =>
    Try {
      val rawId = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
      val id = Uuid[AudioPlay](rawId).get
      AudioPlayToken(id)
    }.toOption

  given TokenEncoder[AudioPlayToken] = token =>
    val raw = token.identity.toString
    Try(
      Base64.getUrlEncoder.withoutPadding.encodeToString(
        raw.getBytes("UTF-8"))).toOption
