package org.aulune
package infrastructure.jdbc.doobie


import domain.model.*

import doobie.Meta

import java.time.Instant
import java.util.UUID


given Meta[MediumType] = Meta[Int].imap { case 1 => MediumType.AudioPlay } {
  case MediumType.AudioPlay => 1
}


given Meta[MediaResourceId] =
  Meta[String].imap(MediaResourceId.unsafeApply)(_.string)


given Meta[AudioPlayTitle] = Meta[String].imap(AudioPlayTitle(_))(_.value)

given Meta[AudioPlaySeriesId] = Meta[Long].imap(AudioPlaySeriesId(_))(_.value)

given Meta[Instant] = Meta[String].imap(Instant.parse)(_.toString)


given Meta[TranslationId] =
  Meta[String].imap(s => TranslationId(UUID.fromString(s)))(_.uuid.toString)


given Meta[TranslationTitle] = Meta[String].imap(TranslationTitle(_))(_.value)
