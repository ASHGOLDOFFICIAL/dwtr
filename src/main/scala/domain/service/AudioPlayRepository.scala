package org.aulune
package domain.service

import domain.model.{AudioPlay, MediaResourceID}

trait AudioPlayRepository[F[_]]
    extends GenericRepository[F, AudioPlay, MediaResourceID]
