package org.aulune
package domain.repo


import domain.model.{Translation, TranslationId, TranslationIdentity}

import java.time.Instant


trait TranslationRepository[F[_]]
    extends GenericRepository[
      F,
      Translation,
      TranslationIdentity,
      (TranslationIdentity, Instant)]
