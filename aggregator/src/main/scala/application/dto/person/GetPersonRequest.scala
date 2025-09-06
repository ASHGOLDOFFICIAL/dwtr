package org.aulune.aggregator
package application.dto.person

import java.util.UUID


/** Request to get a person.
 *  @param name resource identifier.
 */
final case class GetPersonRequest(
    name: UUID,
)
