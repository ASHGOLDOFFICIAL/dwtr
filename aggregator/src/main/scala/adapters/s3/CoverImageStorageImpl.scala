package org.aulune.aggregator
package adapters.s3


import adapters.s3.CoverImageStorageImpl.{
  DefaultContentType,
  IfNoneMatchHeader,
  NoSuchKey,
  PreconditionFailed,
}
import domain.model.shared.ImageUri
import domain.repositories.CoverImageStorage

import cats.MonadThrow
import cats.effect.{Async, Sync}
import cats.syntax.all.given
import fs2.Stream
import fs2.io.{readInputStream, toInputStreamResource}
import io.minio.errors.ErrorResponseException
import io.minio.{
  GetObjectArgs,
  MakeBucketArgs,
  MinioClient,
  PutObjectArgs,
  RemoveObjectArgs,
  StatObjectArgs,
}
import org.aulune.commons.storages.StorageError
import org.aulune.commons.types.NonEmptyString
import org.typelevel.log4cats.syntax.given
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.io.InputStream
import java.net.URI


object CoverImageStorageImpl:
  /** Builds cover image storage.
   *  @param client MinIO client.
   *  @param publicUrl MinIO endpoint to make URIs.
   *  @param bucketName name of bucket to use. Should already exist.
   *  @param partSize part size for object in range [5MiB, 5GiB]. See
   *    [[https://minio-java.min.io/io/minio/PutObjectArgs.Builder.html#stream-java.io.InputStream-long-long- MinIO documentation]].
   *  @tparam F effect type.
   *  @throws IllegalArgumentException when given incorrect part size.
   *  @throws IllegalStateException if bucket doesn't exist.
   */
  def build[F[_]: Async: LoggerFactory](
      client: MinioClient,
      publicUrl: String,
      bucketName: String,
      partSize: Long,
  ): F[CoverImageStorage[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    for
      _ <- MonadThrow[F]
        .raiseWhen(partSize < MinPartSize || partSize > MaxPartSize)(
          new IllegalArgumentException())
        .onError(_ => error"Invalid part size is given.")
      _ <- createBucketIfNoneExists(client, bucketName)
      normalized =
        if publicUrl.endsWith("/") then publicUrl else publicUrl + "/"
    yield CoverImageStorageImpl(client, normalized, bucketName, partSize)

  /** Creates MinIO bucket if none exists.
   *  @param client MinIO client.
   *  @param bucket bucket name.
   *  @tparam F effect type.
   */
  private def createBucketIfNoneExists[F[_]: Sync](
      client: MinioClient,
      bucket: String,
  ): F[Unit] =
    val makeArgs = MakeBucketArgs.builder.bucket(bucket).build
    Sync[F].blocking(client.makeBucket(makeArgs)).recover {
      case e: ErrorResponseException
           if e.errorResponse.code == "BucketAlreadyOwnedByYou" => ()
    }

  /** Minimum allowed part size: 5MiB. */
  val MinPartSize: Int = 5 * 1024 * 1024

  /** Maximum allowed part size: 5GiB. */
  val MaxPartSize: Long = 5L * 1024 * 1024 * 1024

  private val IfNoneMatchHeader = java.util.Map.of("If-None-Match", "*")
  private val DefaultContentType = "application/octet-stream"
  private val NoSuchKey = "NoSuchKey"
  private val PreconditionFailed = "PreconditionFailed"

end CoverImageStorageImpl


/** Object uploader implementation via MinIO.
 *  @param client MinIO client.
 *  @param publicUrl MinIO endpoint, should end with '/'.
 *  @param bucketName MinIO bucket to use.
 *  @param partSize part size for object.
 *  @tparam F effect type.
 */
private final class CoverImageStorageImpl[F[_]: Async](
    client: MinioClient,
    publicUrl: String,
    bucketName: String,
    partSize: Long,
) extends CoverImageStorage[F]:

  override def contains(name: NonEmptyString): F[Boolean] =
    val args = StatObjectArgs.builder
      .bucket(bucketName)
      .`object`(name)
      .build

    Sync[F]
      .blocking(client.statObject(args))
      .as(true)
      .recover {
        case e: ErrorResponseException if e.errorResponse.code == NoSuchKey =>
          false
      }
      .handleErrorWith(toInternalError)
  end contains

  override def put(
      stream: Stream[F, Byte],
      name: NonEmptyString,
      contentType: Option[NonEmptyString],
  ): F[Unit] = toInputStreamResource(stream).use { is =>
    val args = PutObjectArgs.builder
      .contentType(contentType.getOrElse(DefaultContentType))
      .bucket(bucketName)
      .`object`(name)
      .stream(is, -1, partSize)
      .headers(IfNoneMatchHeader)
      .build

    Sync[F]
      .blocking(client.putObject(args))
      .void
      .handleErrorWith {
        case e: ErrorResponseException
             if e.errorResponse.code == PreconditionFailed =>
          StorageError.AlreadyExists.raiseError
        case e => StorageError.Internal(e).raiseError
      }
  }

  override def get(name: NonEmptyString): F[Option[Stream[F, Byte]]] =
    val args = GetObjectArgs.builder
      .bucket(bucketName)
      .`object`(name)
      .build

    Sync[F]
      .blocking(client.getObject(args))
      .map(stream => convertStream(stream).some)
      .recover {
        case e: ErrorResponseException if e.errorResponse.code == NoSuchKey =>
          None
      }
      .handleErrorWith(toInternalError)
  end get

  override def delete(name: NonEmptyString): F[Unit] =
    val args = RemoveObjectArgs.builder
      .bucket(bucketName)
      .`object`(name)
      .build
    Sync[F]
      .blocking(client.removeObject(args))
      .handleErrorWith(toInternalError)

  override def issueURI(name: NonEmptyString): F[Option[ImageUri]] = contains(
    name)
    .map { exists =>
      Option.when(exists)(
        ImageUri.unsafe(URI.create(s"$publicUrl$bucketName/$name")))
    }
    .handleErrorWith(toInternalError)

  /** Converts [[InputStream]] to [[Stream]].
   *  @param is input stream to convert.
   */
  private def convertStream(is: InputStream) =
    readInputStream(is.pure[F], chunkSize = 100, closeAfterUse = true)

  /** Packs all errors to [[StorageError.Internal]]
   *  @param e error to pack.
   *  @tparam A needed return type.
   */
  private def toInternalError[A](e: Throwable) =
    StorageError.Internal(e).raiseError[F, A]
