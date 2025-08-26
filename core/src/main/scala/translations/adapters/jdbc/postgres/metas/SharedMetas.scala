package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.shared.{ImageUrl, ReleaseDate, Synopsis, Uuid}

import cats.Show
import cats.syntax.all.given
import doobie.Meta
import doobie.postgres.implicits.{
  JavaLocalDateMeta,
  UuidType,
  unliftedStringArrayType,
  unliftedUUIDArrayType,
}
import io.circe.parser.parse
import io.circe.syntax.given
import io.circe.{Decoder, Encoder, Json}
import org.postgresql.util.PGobject

import java.net.{URI, URL}
import java.time.LocalDate
import java.util.UUID


/** [[Meta]] instances for Java and shared domain objects. */
private[postgres] object SharedMetas:
  private given Show[LocalDate] = Show.show(_.toString)
  private given Show[URL] = Show.show(_.toString)

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

  given imageUrlMeta: Meta[ImageUrl] = Meta[URL].tiemap { url =>
    ImageUrl(url).toRight(s"Failed to decode ImageUrl from: $url.")
  }(identity)
  given releaseDateMeta: Meta[ReleaseDate] = JavaLocalDateMeta.tiemap { date =>
    ReleaseDate(date).toRight(s"Failed to decode ReleaseDate from: $date.")
  }(identity)
  given synopsisMeta: Meta[Synopsis] = Meta[String].tiemap { str =>
    Synopsis(str).toRight(s"Failed to decode Synopsis from: $str.")
  }(identity)
