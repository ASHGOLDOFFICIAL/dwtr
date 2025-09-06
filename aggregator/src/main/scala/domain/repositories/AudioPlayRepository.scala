package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.AudioPlay
import domain.repositories.AudioPlayRepository.AudioPlayCursor
import org.aulune.aggregator.domain.model.audioplay.series.AudioPlaySeries

import org.aulune.commons.pagination.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{
  GenericRepository,
  PaginatedList,
  TextSearch,
}
import org.aulune.commons.types.Uuid

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import scala.util.Try


/** Repository for [[AudioPlay]] objects.
 *  @tparam F effect type.
 */
trait AudioPlayRepository[F[_]]
    extends GenericRepository[F, AudioPlay, Uuid[AudioPlay]]
    with PaginatedList[F, AudioPlay, AudioPlayCursor]
    with TextSearch[F, AudioPlay]:

  /** Returns audio play series with given ID if found.
   *  @param id audio play series ID.
   */
  def getSeries(id: Uuid[AudioPlaySeries]): F[Option[AudioPlaySeries]]


object AudioPlayRepository:
  /** Cursor to resume pagination of audio plays.
   *  @param id identity of [[AudioPlay]].
   */
  final case class AudioPlayCursor(id: Uuid[AudioPlay])

  given CursorDecoder[AudioPlayCursor] = token =>
    Try {
      val decoded = Base64.getUrlDecoder.decode(token)
      val idString = new String(decoded, UTF_8)
      val id = Uuid[AudioPlay](idString).get
      AudioPlayCursor(id)
    }.toOption

  given CursorEncoder[AudioPlayCursor] = token =>
    val raw = token.id.toString
    Base64.getUrlEncoder.withoutPadding.encodeToString(raw.getBytes(UTF_8))
