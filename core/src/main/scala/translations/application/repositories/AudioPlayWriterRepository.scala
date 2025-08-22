package org.aulune
package translations.application.repositories


import translations.domain.model.audioplay.{AudioPlay, CastMember}
import translations.domain.shared.{Person, Uuid}


/** Repository map between [[Person]]s and [[AudioPlay]] in relation "X is the
 *  writer of Y".
 *  @tparam F effect type.
 */
trait AudioPlayWriterRepository[F[_]]:
  def getById(id: Uuid[AudioPlay]): F[List[Person]]
