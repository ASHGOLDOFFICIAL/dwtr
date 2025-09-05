package org.aulune.commons
package adapters.doobie.postgres


import types.{NonEmptyString, Uuid}

import doobie.Meta
import doobie.postgres.implicits.{
  UuidType,
  unliftedStringArrayType,
  unliftedUUIDArrayType,
}
import io.circe.Json
import io.circe.parser.parse
import org.postgresql.util.PGobject

import java.net.URI
import java.util.UUID


/** [[Meta]] instances for Java and shared types. */
object Metas:
  given uriMeta: Meta[URI] = Meta[String].imap(URI.create)(_.toString)
  given urisMeta: Meta[Array[URI]] = Meta[Array[String]]
    .imap(_.map(URI.create))(_.map(_.toString))

  given nonEmptyStringMeta: Meta[NonEmptyString] =
    Meta[String].imap(NonEmptyString.unsafe)(identity)

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
