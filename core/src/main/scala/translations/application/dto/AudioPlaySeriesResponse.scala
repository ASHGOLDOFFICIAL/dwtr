package org.aulune
package translations.application.dto

import java.util.UUID


/** Audio play series response body.
 * @param id unique series ID.
 * @param name series name.
 */
final case class AudioPlaySeriesResponse(
    id: UUID,
    name: String,
)
