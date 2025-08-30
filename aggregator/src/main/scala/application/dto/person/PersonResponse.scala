package org.aulune.aggregator
package application.dto.person

import java.util.UUID


/** Person response body.
 *  @param id person unique ID.
 *  @param name full name.
 */
final case class PersonResponse(
    id: UUID,
    name: String,
)
