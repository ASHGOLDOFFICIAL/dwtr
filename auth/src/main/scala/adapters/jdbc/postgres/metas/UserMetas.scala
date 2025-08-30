package org.aulune.auth
package adapters.jdbc.postgres.metas


import domain.model.Username

import doobie.Meta


private[postgres] object UserMetas:
  given Meta[Username] = Meta[String].tiemap { str =>
    Username(str)
      .toRight(s"Failed to decode Username from: $str.")
  }(identity)
