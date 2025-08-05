package org.aulune
package translations.application.dto

case class AudioPlayRequest(
    title: String,
    seriesId: Option[Long],
    seriesOrder: Option[Int],
)