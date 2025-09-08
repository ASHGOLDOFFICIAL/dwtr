package org.aulune.aggregator
package adapters.jdbc.postgres.metas


import domain.model.shared.{ImageUri, ReleaseDate, SelfHostedLocation, Synopsis}

import cats.Show
import doobie.Meta
import doobie.postgres.implicits.JavaLocalDateMeta
import org.aulune.commons.adapters.doobie.postgres.Metas.uriMeta

import java.net.URI
import java.time.LocalDate


/** [[Meta]] instances for shared domain objects. */
private[postgres] object SharedMetas:
  private given Show[LocalDate] = Show.show(_.toString)
  private given Show[URI] = Show.show(_.toString)

  given selfHostUriMeta: Meta[SelfHostedLocation] = Meta[URI]
    .imap(SelfHostedLocation.unsafe)(identity)
  given imageUriMeta: Meta[ImageUri] = Meta[URI]
    .imap(ImageUri.unsafe)(identity)

  given releaseDateMeta: Meta[ReleaseDate] = JavaLocalDateMeta
    .imap(ReleaseDate.unsafe)(identity)
  given synopsisMeta: Meta[Synopsis] = Meta[String]
    .imap(Synopsis.unsafe)(identity)
