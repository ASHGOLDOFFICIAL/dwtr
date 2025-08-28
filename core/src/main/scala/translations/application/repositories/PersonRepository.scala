package org.aulune
package translations.application.repositories


import shared.model.Uuid
import shared.repositories.GenericRepository
import translations.domain.model.audioplay.AudioPlay
import translations.domain.model.person.Person


/** Repository for [[Person]] objects.
 *  @tparam F effect type.
 */
trait PersonRepository[F[_]] extends GenericRepository[F, Person, Uuid[Person]]
