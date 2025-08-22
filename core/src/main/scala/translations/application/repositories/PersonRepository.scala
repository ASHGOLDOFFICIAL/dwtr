package org.aulune
package translations.application.repositories


import shared.repositories.GenericRepository
import translations.domain.model.audioplay.AudioPlay
import translations.domain.shared.{Person, Uuid}


/** Repository for [[Person]] objects.
 *  @tparam F effect type.
 */

trait PersonRepository[F[_]] extends GenericRepository[F, Person, Uuid[Person]]
