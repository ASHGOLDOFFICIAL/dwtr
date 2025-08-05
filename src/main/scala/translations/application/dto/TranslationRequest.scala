package org.aulune
package translations.application.dto

import java.net.URI


case class TranslationRequest(
    title: String,
    links: List[URI],
)