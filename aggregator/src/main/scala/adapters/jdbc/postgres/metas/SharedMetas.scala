package org.aulune.aggregator
package adapters.jdbc.postgres.metas


import cats.Show
import doobie.Meta
import doobie.postgres.implicits.JavaLocalDateMeta
import org.aulune.aggregator.domain.model.shared.{
  ImageUri,
  ReleaseDate,
  Synopsis,
}
import org.aulune.commons.adapters.doobie.postgres.Metas.uriMeta

import java.net.{URI, URL}
import java.time.LocalDate


/** [[Meta]] instances for Java and shared domain objects. */
private[postgres] object SharedMetas:
  private given Show[LocalDate] = Show.show(_.toString)
  private given Show[URI] = Show.show(_.toString)

  given imageUrlMeta: Meta[ImageUri] = Meta[URI].tiemap { url =>
    ImageUri(url).toRight(s"Failed to decode ImageUrl from: $url.")
  }(identity)
  given releaseDateMeta: Meta[ReleaseDate] = JavaLocalDateMeta.tiemap { date =>
    ReleaseDate(date).toRight(s"Failed to decode ReleaseDate from: $date.")
  }(identity)
  given synopsisMeta: Meta[Synopsis] = Meta[String].tiemap { str =>
    Synopsis(str).toRight(s"Failed to decode Synopsis from: $str.")
  }(identity)
