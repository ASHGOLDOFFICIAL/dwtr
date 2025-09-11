package org.aulune.aggregator
package adapters.jdbc.postgres.metas


import domain.model.audioplay.{
  ActorRole,
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import domain.model.person.Person

import doobie.Meta
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.syntax.given
import io.circe.{Decoder, Encoder}
import org.aulune.aggregator.domain.model.audioplay.series.AudioPlaySeriesName
import org.aulune.aggregator.domain.model.shared.{
  ExternalResource,
  ExternalResourceType,
}
import org.aulune.commons.adapters.doobie.postgres.Metas.jsonbMeta
import org.aulune.commons.types.Uuid

import java.net.URL


/** [[Meta]] instances for [[AudioPlay]]. */
private[postgres] object AudioPlayMetas:
  given actorRoleMeta: Meta[ActorRole] =
    Meta[String].imap(ActorRole.unsafe)(identity)

  given audioPlayTitleMeta: Meta[AudioPlayTitle] =
    Meta[String].imap(AudioPlayTitle.unsafe)(identity)

  given audioPlaySeasonMeta: Meta[AudioPlaySeason] =
    Meta[Int].imap(AudioPlaySeason.unsafe)(identity)

  given audioPlaySeriesNameMeta: Meta[AudioPlaySeriesName] =
    Meta[String].imap(AudioPlaySeriesName.unsafe)(identity)

  given audioPlaySeriesNumberMeta: Meta[AudioPlaySeriesNumber] =
    Meta[Int].imap(AudioPlaySeriesNumber.unsafe)(identity)

  given castMemberMeta: Meta[CastMember] = jsonbMeta.imap(json =>
    json.as[CastMember].fold(throw _, identity))(_.asJson)
  given castMembersMeta: Meta[List[CastMember]] = jsonbMeta.imap(json =>
    json.as[List[CastMember]].fold(throw _, identity))(_.asJson)

  given writersMeta: Meta[List[Uuid[Person]]] = jsonbMeta.imap(json =>
    json.as[List[Uuid[Person]]].fold(throw _, identity))(_.asJson)

  private given Configuration = Configuration.default.withSnakeCaseMemberNames
  private given [A]: Decoder[Uuid[A]] = Decoder.decodeUUID.map(Uuid[A])
  private given [A]: Encoder[Uuid[A]] = Encoder.encodeUUID.contramap(identity)
  private given Decoder[ActorRole] = Decoder.decodeString.map(ActorRole.unsafe)
  private given Encoder[ActorRole] = Encoder.encodeString.contramap(identity)
  private given Decoder[CastMember] = deriveConfiguredDecoder
  private given Encoder[CastMember] = deriveConfiguredEncoder
