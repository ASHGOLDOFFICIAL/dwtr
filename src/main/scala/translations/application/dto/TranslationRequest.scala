package org.aulune
package translations.application.dto

import java.net.URI


/** Translation request body.
 *
 *  @param title translated title.
 *  @param links links to where translation is published.
 */
case class TranslationRequest(
    title: String,
    links: List[URI],
)
