package org.aulune
package domain.model


case class AudioPlayRequest(
    title: String,
    seriesId: Option[Long],
    seriesOrder: Option[Int],
)