package org.aulune.aggregator
package application.dto.person

import java.util.UUID


/** Person response body.
 *  @param id person unique ID.
 *  @param name full name.
 */
final case class PersonResource(
    id: UUID,
    name: String,
)
