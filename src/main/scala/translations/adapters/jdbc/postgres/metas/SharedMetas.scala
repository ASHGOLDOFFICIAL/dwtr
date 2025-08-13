package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.shared.Uuid

import doobie.Meta
import doobie.postgres.implicits.*

import java.util.UUID


private[postgres] object SharedMetas:
  given [A]: Meta[Uuid[A]] = Meta[UUID].imap(Uuid[A].apply)(identity)
