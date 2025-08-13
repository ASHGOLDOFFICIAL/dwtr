package org.aulune
package translations.domain.shared

import java.net.{URI, URL}

/** Link to an external resource.
 *  @param resourceType type of external resource.
 *  @param url link to it.
 */
final case class ExternalResource(resourceType: ExternalResourceType, url: URL)
