package org.aulune
package domain.repo


import domain.model.{AudioPlay, MediaResourceID}

import java.time.Instant


trait AudioPlayRepository[F[_]]
    extends GenericRepository[
      F,
      AudioPlay,
      MediaResourceID,
      (MediaResourceID, Instant)]
