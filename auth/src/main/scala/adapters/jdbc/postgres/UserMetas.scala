package org.aulune.auth
package adapters.jdbc.postgres


import domain.model.{ExternalId, Username}

import doobie.Meta


/** [[Meta]] instances for user object. */
private[postgres] object UserMetas:
  given Meta[Username] = Meta[String]
    .imap(str => Username.unsafe(str))(identity)

  given Meta[ExternalId] = Meta[String]
    .imap(str => ExternalId.unsafe(str))(identity)
