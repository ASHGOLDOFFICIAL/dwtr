package org.aulune
package aggregator.application.dto

import java.net.URL


/** Link to an external resource.
 *
 *  @param resourceType type of external resource.
 *  @param link link to it.
 */
final case class ExternalResourceDto(
    resourceType: ExternalResourceTypeDto,
    link: URL,
)
