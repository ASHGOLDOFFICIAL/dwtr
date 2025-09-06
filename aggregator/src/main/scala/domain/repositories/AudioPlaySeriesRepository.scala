package org.aulune.aggregator
package domain.repositories


import domain.model.audioplay.series.AudioPlaySeries
import domain.repositories.AudioPlaySeriesRepository.Cursor

import org.aulune.commons.pagination.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{
  GenericRepository,
  PaginatedList,
  TextSearch,
}
import org.aulune.commons.types.Uuid

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import scala.util.Try


trait AudioPlaySeriesRepository[F[_]]
    extends GenericRepository[F, AudioPlaySeries, Uuid[AudioPlaySeries]]
    with PaginatedList[F, AudioPlaySeries, Cursor]
    with TextSearch[F, AudioPlaySeries]


object AudioPlaySeriesRepository:
  /** Cursor to resume pagination.
   *  @param id identity of last entry.
   */
  final case class Cursor(id: Uuid[AudioPlaySeries])

  given CursorDecoder[Cursor] = token =>
    Try {
      val decoded = Base64.getUrlDecoder.decode(token)
      val idString = new String(decoded, UTF_8)
      val id = Uuid[AudioPlaySeries](idString).get
      Cursor(id)
    }.toOption

  given CursorEncoder[Cursor] = token =>
    val raw = token.id.toString
    Base64.getUrlEncoder.withoutPadding.encodeToString(raw.getBytes(UTF_8))
