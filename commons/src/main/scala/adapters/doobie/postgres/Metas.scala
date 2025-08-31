package org.aulune.commons
package adapters.doobie.postgres

import doobie.Meta
import doobie.postgres.implicits.{
  UuidType,
  unliftedStringArrayType,
  unliftedUUIDArrayType,
}
import types.Uuid

import io.circe.Json
import io.circe.parser.parse
import org.postgresql.util.PGobject

import java.net.{URI, URL}
import java.util.UUID


/** [[Meta]] instances for Java and shared types. */
object Metas:
  given urlMeta: Meta[URL] = Meta[String].imap(URI.create(_).toURL)(_.toString)
  given urlsMeta: Meta[Array[URL]] = Meta[Array[String]]
    .imap(_.map(URI.create(_).toURL))(_.map(_.toString))

  given jsonbMeta: Meta[Json] = Meta.Advanced
    .other[PGobject]("jsonb")
    .timap[Json](obj => parse(obj.getValue).fold(throw _, identity)) { json =>
      val obj = new PGobject
      obj.setType("json")
      obj.setValue(json.noSpaces)
      obj
    }

  given uuidMeta[A]: Meta[Uuid[A]] = Meta[UUID].imap(Uuid[A].apply)(identity)
  given uuidsMeta[A]: Meta[Array[Uuid[A]]] = Meta[Array[UUID]]
    .imap(_.map(Uuid[A].apply))(_.map(identity))
