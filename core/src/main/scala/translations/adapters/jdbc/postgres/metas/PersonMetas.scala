package org.aulune
package translations.adapters.jdbc.postgres.metas


import translations.domain.model.person.{FullName, Person}

import doobie.Meta


/** [[Meta]] instances for [[Person]]. */
private[postgres] object PersonMetas:
  given Meta[FullName] = Meta[String].timap(FullName.unsafe)(identity)
