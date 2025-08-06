package org.aulune
package translations.domain.repositories


import shared.repositories.GenericRepository
import translations.domain.model.translation.{Translation, TranslationIdentity}

import java.time.Instant


trait TranslationRepository[F[_]]
    extends GenericRepository[
      F,
      Translation,
      TranslationIdentity,
      (TranslationIdentity, Instant)]
