package org.aulune
package aggregator.adapters.jdbc.postgres.metas

import aggregator.domain.model.person.FullName

import doobie.Meta


/** [[Meta]] instances for [[Person]]. */
private[postgres] object PersonMetas:
  given Meta[FullName] = Meta[String].imap(FullName.unsafe)(identity)
