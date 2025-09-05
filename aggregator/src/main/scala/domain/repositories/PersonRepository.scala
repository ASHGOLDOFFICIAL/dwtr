package org.aulune.aggregator
package domain.repositories


import domain.model.person.Person

import org.aulune.commons.repositories.{BatchGet, GenericRepository}
import org.aulune.commons.types.Uuid


/** Repository for [[Person]] objects.
 *  @tparam F effect type.
 */
trait PersonRepository[F[_]]
    extends GenericRepository[F, Person, Uuid[Person]]
    with BatchGet[F, Person, Uuid[Person]]
