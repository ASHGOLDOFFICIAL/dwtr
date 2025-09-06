package org.aulune.aggregator
package application.dto.person

import java.util.UUID


/** Request to delete a person.
 *  @param name resource identifier.
 */
final case class DeletePersonRequest(
    name: UUID,
)
