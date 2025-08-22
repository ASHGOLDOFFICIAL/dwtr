package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.shared.{ImageUrl, ReleaseDate, Synopsis, Uuid}

import cats.Show
import doobie.Meta
import doobie.postgres.implicits.*

import java.net.{URI, URL}
import java.time.LocalDate
import java.util.UUID


private[postgres] object SharedMetas:
  private given Show[LocalDate] = Show.show(_.toString)
  private given Show[URL] = Show.show(_.toString)

  given Meta[URL] = Meta[String].imap(URI.create(_).toURL)(_.toString)
  given Meta[Array[URL]] = Meta[Array[String]]
    .timap(_.map(URI.create(_).toURL))(_.map(_.toString))

  given [A]: Meta[Uuid[A]] = Meta[UUID].imap(Uuid[A].apply)(identity)
  given Meta[ImageUrl] = Meta[URL].tiemap { url =>
    ImageUrl(url).toRight(s"Failed to decode ImageUrl from: $url.")
  }(identity)
  given Meta[ReleaseDate] = JavaLocalDateMeta.tiemap { date =>
    ReleaseDate(date).toRight(s"Failed to decode ReleaseDate from: $date.")
  }(identity)
  given Meta[Synopsis] = Meta[String].tiemap { str =>
    Synopsis(str).toRight(s"Failed to decode Synopsis from: $str.")
  }(identity)
