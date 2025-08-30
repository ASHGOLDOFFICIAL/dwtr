package org.aulune.aggregator
package adapters.jdbc.postgres.metas


import domain.model.person.FullName

import doobie.Meta


/** [[Meta]] instances for [[Person]]. */
private[postgres] object PersonMetas:
  given Meta[FullName] = Meta[String].imap(FullName.unsafe)(identity)
