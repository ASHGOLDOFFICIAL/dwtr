package org.aulune
package domain.repo

import domain.model.{Translation, TranslationId}

trait TranslationRepository[F[_]]
    extends GenericRepository[F, Translation, TranslationId]
