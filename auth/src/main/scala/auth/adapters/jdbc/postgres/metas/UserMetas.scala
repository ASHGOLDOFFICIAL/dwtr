package org.aulune
package auth.adapters.jdbc.postgres.metas

import auth.domain.model.Username

import doobie.Meta


private[postgres] object UserMetas:
  given Meta[Username] = Meta[String].tiemap { str =>
    Username(str)
      .toRight(s"Failed to decode Username from: $str.")
  }(identity)
