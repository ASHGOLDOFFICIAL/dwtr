package org.aulune
package domain.repo

import domain.model.*

trait TranslationRepository[F[_]]
    extends GenericRepository[F, Translation, TranslationIdentity]
