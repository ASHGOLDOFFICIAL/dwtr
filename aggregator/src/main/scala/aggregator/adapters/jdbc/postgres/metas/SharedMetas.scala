package org.aulune
package aggregator.adapters.jdbc.postgres.metas

import shared.adapters.jdbc.postgres.metas.SharedMetas.urlMeta
import aggregator.domain.shared.{ImageUrl, ReleaseDate, Synopsis}

import cats.Show
import doobie.Meta
import doobie.postgres.implicits.JavaLocalDateMeta

import java.net.URL
import java.time.LocalDate


/** [[Meta]] instances for Java and shared domain objects. */
private[postgres] object SharedMetas:
  private given Show[LocalDate] = Show.show(_.toString)
  private given Show[URL] = Show.show(_.toString)

  given imageUrlMeta: Meta[ImageUrl] = Meta[URL].tiemap { url =>
    ImageUrl(url).toRight(s"Failed to decode ImageUrl from: $url.")
  }(identity)
  given releaseDateMeta: Meta[ReleaseDate] = JavaLocalDateMeta.tiemap { date =>
    ReleaseDate(date).toRight(s"Failed to decode ReleaseDate from: $date.")
  }(identity)
  given synopsisMeta: Meta[Synopsis] = Meta[String].tiemap { str =>
    Synopsis(str).toRight(s"Failed to decode Synopsis from: $str.")
  }(identity)
