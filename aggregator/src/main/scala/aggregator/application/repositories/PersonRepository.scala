package org.aulune
package aggregator.application.repositories

import shared.model.Uuid
import shared.repositories.GenericRepository
import aggregator.domain.model.audioplay.AudioPlay
import aggregator.domain.model.person.Person


/** Repository for [[Person]] objects.
 *  @tparam F effect type.
 */
trait PersonRepository[F[_]] extends GenericRepository[F, Person, Uuid[Person]]
