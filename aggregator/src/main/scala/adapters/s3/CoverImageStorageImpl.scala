package org.aulune.aggregator
package adapters.s3


import domain.model.shared.ImageUri
import domain.repositories.CoverImageStorage

import cats.MonadThrow
import cats.effect.std.UUIDGen
import cats.effect.{Async, Sync}
import cats.syntax.all.given
import fs2.Stream
import fs2.io.{readInputStream, toInputStreamResource}
import io.minio.errors.ErrorResponseException
import io.minio.{
  BucketExistsArgs,
  GetObjectArgs,
  MinioClient,
  PutObjectArgs,
  RemoveObjectArgs,
  StatObjectArgs,
}
import org.aulune.commons.types.NonEmptyString
import org.typelevel.log4cats.syntax.given
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.io.InputStream
import java.net.URI
import scala.util.{Failure, Success, Try}


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
      exist <- checkIfBucketExists(client, bucketName)
      _ <- MonadThrow[F]
        .raiseWhen(partSize < MinPartSize || partSize > MaxPartSize)(
          new IllegalArgumentException())
        .onError(_ => error"Invalid part size is given.")
      _ <- MonadThrow[F]
        .raiseUnless(exist)(new IllegalStateException())
        .onError(_ => error"Bucket $bucketName doesn't exists.")
      normalized =
        if publicUrl.endsWith("/") then publicUrl else publicUrl + "/"
    yield CoverImageStorageImpl(client, normalized, bucketName, partSize)

  /** Checks if MinIO bucket exists.
   *  @param client MinIO client.
   *  @param bucket bucket name.
   *  @tparam F effect type.
   *  @return `true` if bucket exists, otherwise `false`.
   */
  private def checkIfBucketExists[F[_]: Sync](
      client: MinioClient,
      bucket: String,
  ): F[Boolean] =
    val bucketArgs = BucketExistsArgs.builder.bucket(bucket).build
    Sync[F].blocking(client.bucketExists(bucketArgs))

  /** Minimum allowed part size: 5MiB. */
  val MinPartSize: Int = 5 * 1024 * 1024

  /** Maximum allowed part size: 5GiB. */
  val MaxPartSize: Long = 5L * 1024 * 1024 * 1024

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

  private val defaultContentType = "application/octet-stream"

  override def contains(name: NonEmptyString): F[Boolean] =
    val args = StatObjectArgs.builder
      .bucket(bucketName)
      .`object`(name)
      .build

    Sync[F]
      .blocking(Try(client.statObject(args)))
      .flatMap {
        case Failure(e: ErrorResponseException)
             if e.errorResponse.code == "NoSuchKey" => false.pure[F]
        case Failure(e)     => MonadThrow[F].raiseError(e)
        case Success(value) => true.pure[F]
      }
  end contains

  override def put(
      stream: Stream[F, Byte],
      name: NonEmptyString,
      contentType: Option[NonEmptyString],
  ): F[Unit] = toInputStreamResource(stream).use { is =>
    val args = PutObjectArgs.builder
      .contentType(contentType.getOrElse(defaultContentType))
      .bucket(bucketName)
      .`object`(name)
      .stream(is, -1, partSize)
      .build
    Sync[F].blocking(client.putObject(args))
  }

  override def get(name: NonEmptyString): F[Option[Stream[F, Byte]]] =
    val args = GetObjectArgs.builder
      .bucket(bucketName)
      .`object`(name)
      .build

    Sync[F]
      .blocking(Try(client.getObject(args)))
      .flatMap {
        case Failure(e: ErrorResponseException)
             if e.errorResponse.code == "NoSuchKey" => None.pure[F]
        case Failure(e)     => MonadThrow[F].raiseError[Option[InputStream]](e)
        case Success(value) => value.some.pure[F]
      }
      .map(_.map(convertStream))
  end get

  override def delete(name: NonEmptyString): F[Unit] =
    val args = RemoveObjectArgs.builder
      .bucket(bucketName)
      .`object`(name)
      .build
    Sync[F].blocking(client.removeObject(args))

  override def issueURI(name: NonEmptyString): F[Option[ImageUri]] = contains(
    name).map { exists =>
    Option.when(exists)(
      ImageUri.unsafe(URI.create(s"$publicUrl$bucketName/$name")))
  }

  /** Converts [[InputStream]] to [[Stream]].
   *  @param is input stream to convert.
   */
  private def convertStream(is: InputStream) =
    readInputStream(is.pure[F], chunkSize = 100, closeAfterUse = true)
