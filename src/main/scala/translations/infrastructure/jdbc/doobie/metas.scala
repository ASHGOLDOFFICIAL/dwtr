package org.aulune
package translations.infrastructure.jdbc.doobie

import translations.domain.model.audioplay.{AudioPlaySeriesId, AudioPlayTitle}
import translations.domain.model.shared.MediaResourceId
import translations.domain.model.translation.{
  MediumType,
  TranslationId,
  TranslationTitle
}

import doobie.Meta

import java.time.Instant
import java.util.UUID


given Meta[MediaResourceId] =
  Meta[String].imap(MediaResourceId.unsafeApply)(_.string)


given Meta[AudioPlayTitle] = Meta[String].imap(AudioPlayTitle(_))(_.value)

given Meta[AudioPlaySeriesId] = Meta[Long].imap(AudioPlaySeriesId(_))(_.value)

given Meta[Instant] = Meta[String].imap(Instant.parse)(_.toString)


given Meta[MediumType] = Meta[Int].imap { case 1 => MediumType.AudioPlay } {
  case MediumType.AudioPlay => 1
}


given Meta[TranslationId] =
  Meta[String].imap(TranslationId.unsafeApply)(_.string)


given Meta[TranslationTitle] = Meta[String].imap(TranslationTitle(_))(_.value)
