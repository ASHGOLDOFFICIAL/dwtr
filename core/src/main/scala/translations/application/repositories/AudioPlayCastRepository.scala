package org.aulune
package translations.application.repositories


import shared.repositories.GenericRepository
import translations.domain.model.audioplay.{AudioPlay, CastMember}
import translations.domain.shared.Uuid


/** Repository map between [[Person]]s and [[AudioPlay]] in relation "X is being
 *  a cast member of Y".
 *  @tparam F effect type.
 */
trait AudioPlayCastRepository[F[_]]:
  def getById(id: Uuid[AudioPlay]): F[List[CastMember]]
