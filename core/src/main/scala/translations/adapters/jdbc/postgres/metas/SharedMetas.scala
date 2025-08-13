package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.shared.Uuid

import doobie.Meta
import doobie.postgres.implicits.*

import java.net.{URI, URL}
import java.util.UUID


private[postgres] object SharedMetas:
  given Meta[URL] = Meta[String].imap(URI.create(_).toURL)(_.toString)
  given [A]: Meta[Uuid[A]] = Meta[UUID].imap(Uuid[A].apply)(identity)
  given Meta[Array[URL]] = Meta[Array[String]]
    .timap(_.map(URI.create(_).toURL))(_.map(_.toString))
