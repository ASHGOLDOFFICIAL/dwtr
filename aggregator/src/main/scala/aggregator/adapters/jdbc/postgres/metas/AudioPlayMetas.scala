package org.aulune
package aggregator.adapters.jdbc.postgres.metas

import commons.adapters.jdbc.postgres.metas.SharedMetas.jsonbMeta
import commons.types.Uuid
import aggregator.domain.model.audioplay.{
  ActorRole,
  AudioPlay,
  AudioPlaySeason,
  AudioPlaySeriesName,
  AudioPlaySeriesNumber,
  AudioPlayTitle,
  CastMember,
}
import aggregator.domain.model.person.Person
import aggregator.domain.shared.{ExternalResource, ExternalResourceType}

import doobie.Meta
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.syntax.given
import io.circe.{Decoder, Encoder}

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

  private val resourceTypeToInt = ExternalResourceType.values.map {
    case t @ ExternalResourceType.Purchase  => t -> 1
    case t @ ExternalResourceType.Streaming => t -> 2
    case t @ ExternalResourceType.Download  => t -> 3
    case t @ ExternalResourceType.Other     => t -> 4
    case t @ ExternalResourceType.Private   => t -> 5
  }.toMap
  private val resourceTypeFromInt = resourceTypeToInt.map(_.swap)

  given externalResourceTypeMeta: Meta[ExternalResourceType] = Meta[Int]
    .imap(resourceTypeFromInt.apply)(resourceTypeToInt.apply)

  given castMemberMeta: Meta[CastMember] = jsonbMeta.imap(json =>
    json.as[CastMember].fold(throw _, identity))(_.asJson)
  given castMembersMeta: Meta[List[CastMember]] = jsonbMeta.imap(json =>
    json.as[List[CastMember]].fold(throw _, identity))(_.asJson)

  given externalResourceMeta: Meta[ExternalResource] = jsonbMeta.imap(json =>
    json.as[ExternalResource].fold(throw _, identity))(_.asJson)
  given externalResourcesMeta: Meta[List[ExternalResource]] = jsonbMeta.imap(
    json => json.as[List[ExternalResource]].fold(throw _, identity))(_.asJson)

  given writersMeta: Meta[List[Uuid[Person]]] = jsonbMeta.imap(json =>
    json.as[List[Uuid[Person]]].fold(throw _, identity))(_.asJson)

  private given Configuration = Configuration.default.withSnakeCaseMemberNames
  private given [A]: Decoder[Uuid[A]] = Decoder.decodeUUID.map(Uuid[A])
  private given [A]: Encoder[Uuid[A]] = Encoder.encodeUUID.contramap(identity)
  private given Decoder[ActorRole] = Decoder.decodeString.map(ActorRole.unsafe)
  private given Encoder[ActorRole] = Encoder.encodeString.contramap(identity)
  private given Decoder[ExternalResourceType] =
    Decoder.decodeInt.map(resourceTypeFromInt)
  private given Encoder[ExternalResourceType] =
    Encoder.encodeInt.contramap(resourceTypeToInt)
  private given Decoder[URL] = Decoder.decodeURI.map(_.toURL)
  private given Encoder[URL] = Encoder.encodeURI.contramap(_.toURI)
  private given Decoder[CastMember] = deriveConfiguredDecoder
  private given Encoder[CastMember] = deriveConfiguredEncoder
  private given Decoder[ExternalResource] = deriveConfiguredDecoder
  private given Encoder[ExternalResource] = deriveConfiguredEncoder
