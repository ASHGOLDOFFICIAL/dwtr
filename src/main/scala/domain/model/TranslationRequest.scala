package org.aulune
package domain.model


import java.net.URI


case class TranslationRequest(
    title: String,
    links: List[URI],
)