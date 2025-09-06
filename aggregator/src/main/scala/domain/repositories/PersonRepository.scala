package org.aulune.aggregator
package domain.repositories


import domain.model.person.Person
import domain.repositories.PersonRepository.Cursor

import org.aulune.commons.pagination.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{
  BatchGet,
  GenericRepository,
  PaginatedList,
  TextSearch,
}
import org.aulune.commons.types.Uuid

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
import scala.util.Try


/** Repository for [[Person]] objects.
 *  @tparam F effect type.
 */
trait PersonRepository[F[_]]
    extends GenericRepository[F, Person, Uuid[Person]]
    with BatchGet[F, Person, Uuid[Person]]
    with PaginatedList[F, Person, Cursor]
    with TextSearch[F, Person]


object PersonRepository:
  /** Cursor to resume pagination.
   *  @param id identity of last entry.
   */
  final case class Cursor(id: Uuid[Person])

  given CursorDecoder[Cursor] = token =>
    Try {
      val decoded = Base64.getUrlDecoder.decode(token)
      val idString = new String(decoded, UTF_8)
      val id = Uuid[Person](idString).get
      Cursor(id)
    }.toOption

  given CursorEncoder[Cursor] = token =>
    val raw = token.id.toString
    Base64.getUrlEncoder.withoutPadding.encodeToString(raw.getBytes(UTF_8))
