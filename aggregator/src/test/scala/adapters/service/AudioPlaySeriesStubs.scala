package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlaySeriesServiceErrorResponses
import adapters.service.mappers.AudioPlaySeriesMapper
import application.AudioPlaySeriesService
import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  BatchGetAudioPlaySeriesRequest,
  BatchGetAudioPlaySeriesResponse,
  CreateAudioPlaySeriesRequest,
  DeleteAudioPlaySeriesRequest,
  GetAudioPlaySeriesRequest,
  ListAudioPlaySeriesRequest,
  ListAudioPlaySeriesResponse,
  SearchAudioPlaySeriesRequest,
  SearchAudioPlaySeriesResponse,
}
import domain.model.audioplay.series.{AudioPlaySeries, AudioPlaySeriesName}

import cats.Applicative
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User
import org.aulune.commons.types.Uuid

import java.util.UUID


/** [[AudioPlaySeries]] objects to use in tests. */
private[aggregator] object AudioPlaySeriesStubs:
  /** ''Mega Series'' audio play series. */
  val series1: AudioPlaySeries = AudioPlaySeries
    .unsafe(
      id = Uuid.unsafe("3669ae36-b459-448e-a51e-f8bbc3a41b79"),
      name = AudioPlaySeriesName.unsafe("Mega Series"),
    )

  /** ''Super-soap-drama'' audio play series. */
  val series2: AudioPlaySeries = AudioPlaySeries
    .unsafe(
      id = Uuid.unsafe("8dddf9f1-3f59-41bb-b9b4-c97c861913c2"),
      name = AudioPlaySeriesName.unsafe("Super-soap-drama-series"),
    )

  /** ''Super Series'' audio play series. */
  val series3: AudioPlaySeries = AudioPlaySeries
    .unsafe(
      id = Uuid.unsafe("dfaf0048-7d42-4fe5-b221-aec7aa5da90c"),
      name = AudioPlaySeriesName.unsafe("Super Series"),
    )

  val resourceById: Map[UUID, AudioPlaySeriesResource] =
    val elements = List(series1, series2, series3)
    elements.map(s => s.id -> AudioPlaySeriesMapper.toResponse(s)).toMap

  /** Stub [[AudioPlaySeriesService]] implementation that supports only `get`
   *  and `batchGet` operation.
   *
   *  Contains only series given in [[AudioPlaySeriesStubs]] object.
   *
   *  @tparam F effect type.
   */
  def service[F[_]: Applicative]: AudioPlaySeriesService[F] =
    new AudioPlaySeriesService[F]:
      override def get(
          request: GetAudioPlaySeriesRequest,
      ): F[Either[ErrorResponse, AudioPlaySeriesResource]] = resourceById
        .get(request.name)
        .toRight(AudioPlaySeriesServiceErrorResponses.seriesNotFound)
        .pure[F]

      override def batchGet(
          request: BatchGetAudioPlaySeriesRequest,
      ): F[Either[ErrorResponse, BatchGetAudioPlaySeriesResponse]] =
        val series = request.names.mapFilter(resourceById.get)
        BatchGetAudioPlaySeriesResponse(series).asRight.pure[F]

      override def list(
          request: ListAudioPlaySeriesRequest,
      ): F[Either[ErrorResponse, ListAudioPlaySeriesResponse]] =
        throw new UnsupportedOperationException()

      override def search(
          request: SearchAudioPlaySeriesRequest,
      ): F[Either[ErrorResponse, SearchAudioPlaySeriesResponse]] =
        throw new UnsupportedOperationException()

      override def create(
          user: User,
          request: CreateAudioPlaySeriesRequest,
      ): F[Either[ErrorResponse, AudioPlaySeriesResource]] =
        throw new UnsupportedOperationException()

      override def delete(
          user: User,
          request: DeleteAudioPlaySeriesRequest,
      ): F[Either[ErrorResponse, Unit]] =
        throw new UnsupportedOperationException()
