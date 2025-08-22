package org.aulune
package translations.adapters.jdbc.postgres


import translations.application.repositories.{
  AudioPlayMetadata,
  AudioPlayMetadataRepository,
  AudioPlayRepository
}
import translations.domain.model.audioplay.{AudioPlay, AudioPlaySeries}
import translations.domain.shared.Uuid

import cats.Monad
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.Transactor


/** [[AudioPlayRepository]] implementation for PostgreSQL.
 *  @note It serves as facade to different repositories.
 */
object AudioPlayRepositoryImpl:
  /** Builds [[AudioPlayRepository]] by building several different repositories
   *  with specific responsibility.
   *  @param transactor [[Transactor]] instance to pass to other repositories.
   *  @tparam F effect type.
   */
  def build[F[_]: MonadCancelThrow](
      transactor: Transactor[F],
  ): F[AudioPlayRepository[F]] =
    for metadataRepo <- AudioPlayMetadataRepositoryImpl.build(transactor)
    yield new AudioPlayRepositoryImpl[F](metadataRepo)


private final class AudioPlayRepositoryImpl[F[_]: Monad](
    metadataRepo: AudioPlayMetadataRepository[F],
) extends AudioPlayRepository[F]:

  override def contains(id: Uuid[AudioPlay]): F[Boolean] =
    metadataRepo.contains(id)

  override def get(id: Uuid[AudioPlay]): F[Option[AudioPlay]] =
    for
      metadata <- metadataRepo.get(id)
      result = metadata.map(makeAudioPlay)
    yield result

  override def persist(elem: AudioPlay): F[AudioPlay] =
    val metadata = extractMetadata(elem)
    for result <- metadataRepo.persist(metadata)
    yield elem

  override def update(elem: AudioPlay): F[AudioPlay] =
    val metadata = extractMetadata(elem)
    for result <- metadataRepo.update(metadata)
    yield elem

  override def delete(id: Uuid[AudioPlay]): F[Unit] =
    for _ <- metadataRepo.delete(id)
    yield ()

  override def list(
      startWith: Option[AudioPlayRepository.AudioPlayToken],
      count: Int,
  ): F[List[AudioPlay]] =
    for
      metadata <- metadataRepo.list(startWith, count)
      audios = metadata.map(makeAudioPlay)
    yield audios

  override def getSeries(
      id: Uuid[AudioPlaySeries],
  ): F[Option[AudioPlaySeries]] = metadataRepo.getSeries(id)

  /** Makes [[AudioPlayMetadata]] from [[AudioPlay]].
   *  @param audioPlay audio play to extract metadata from.
   */
  private def extractMetadata(audioPlay: AudioPlay): AudioPlayMetadata =
    AudioPlayMetadata(
      id = audioPlay.id,
      title = audioPlay.title,
      synopsis = audioPlay.synopsis,
      releaseDate = audioPlay.releaseDate,
      series = audioPlay.series,
      seriesSeason = audioPlay.seriesSeason,
      seriesNumber = audioPlay.seriesNumber,
      coverUrl = audioPlay.coverUrl,
      externalResources = audioPlay.externalResources,
    )

  /** Makes [[AudioPlay]] out of given components.
   *  @param meta metadata component.
   */
  private def makeAudioPlay(meta: AudioPlayMetadata): AudioPlay =
    AudioPlay.unsafe(
      id = meta.id,
      title = meta.title,
      synopsis = meta.synopsis,
      releaseDate = meta.releaseDate,
      series = meta.series,
      seriesSeason = meta.seriesSeason,
      seriesNumber = meta.seriesNumber,
      coverUrl = meta.coverUrl,
      externalResources = meta.externalResources,
    )
