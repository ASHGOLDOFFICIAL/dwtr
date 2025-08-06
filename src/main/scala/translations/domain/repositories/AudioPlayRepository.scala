package org.aulune
package translations.domain.repositories


import shared.repositories.GenericRepository
import translations.domain.model.audioplay.AudioPlay
import translations.domain.model.shared.MediaResourceId

import java.time.Instant


trait AudioPlayRepository[F[_]]
    extends GenericRepository[
      F,
      AudioPlay,
      MediaResourceId,
      (MediaResourceId, Instant)]
