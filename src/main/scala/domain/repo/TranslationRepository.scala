package org.aulune
package domain.repo

import domain.model.{Translation, TranslationIdentity}


trait TranslationRepository[F[_]]
    extends GenericRepository[F, Translation, TranslationIdentity]
