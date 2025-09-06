package org.aulune.aggregator
package domain.model.shared

import java.net.URI

/** Link to an external resource.
 *  @param resourceType type of external resource.
 *  @param uri link to it.
 */
final case class ExternalResource(resourceType: ExternalResourceType, uri: URI)
