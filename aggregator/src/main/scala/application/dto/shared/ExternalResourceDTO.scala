package org.aulune.aggregator
package application.dto.shared

import java.net.URI


/** Link to an external resource.
 *
 *  @param resourceType type of external resource.
 *  @param link link to it.
 */
final case class ExternalResourceDTO(
    resourceType: ExternalResourceTypeDTO,
    link: URI,
)
