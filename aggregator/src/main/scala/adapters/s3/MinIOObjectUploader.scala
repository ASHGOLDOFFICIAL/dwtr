package org.aulune.aggregator
package adapters.s3


import application.ObjectUploader

import cats.MonadThrow
import cats.effect.std.UUIDGen
import cats.effect.{Async, Sync}
import cats.syntax.all.given
import fs2.Stream
import fs2.io.toInputStreamResource
import io.minio.{BucketExistsArgs, MinioClient, PutObjectArgs}
import org.aulune.commons.types.NonEmptyString
import org.typelevel.log4cats.syntax.given
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.io.InputStream
import java.net.URI


object MinIOObjectUploader:
  /** Builds MinIO file uploader.
   *  @param client MinIO client.
   *  @param publicUrl MinIO endpoint to make URIs.
   *  @param bucketName name of bucket to use. Should already exist.
   *  @param partSize part size for object in range [5MiB, 5GiB]. See
   *    [[https://minio-java.min.io/io/minio/PutObjectArgs.Builder.html#stream-java.io.InputStream-long-long- MinIO documentation]].
   *  @tparam F effect type.
   *  @throws IllegalArgumentException when given incorrect part size.
   *  @throws IllegalStateException if bucket doesn't exist.
   */
  def build[F[_]: Async: UUIDGen: LoggerFactory](
      client: MinioClient,
      publicUrl: String,
      bucketName: String,
      partSize: Long,
  ): F[ObjectUploader[F]] =
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
    yield MinIOObjectUploader(client, normalized, bucketName, partSize)

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

end MinIOObjectUploader


/** Object uploader implementation via MinIO.
 *  @param client MinIO client.
 *  @param publicUrl MinIO endpoint, should end with '/'.
 *  @param bucketName MinIO bucket to use.
 *  @param partSize part size for object.
 *  @tparam F effect type.
 */
private final class MinIOObjectUploader[F[_]: Async: UUIDGen: LoggerFactory](
    client: MinioClient,
    publicUrl: String,
    bucketName: String,
    partSize: Long,
) extends ObjectUploader[F]:

  private val defaultContentType = "application/octet-stream"
  private given Logger[F] = LoggerFactory[F].getLogger

  override def upload(
      stream: Stream[F, Byte],
      contentType: Option[NonEmptyString],
      extension: Option[NonEmptyString] = None,
  ): F[URI] =
    for
      id <- UUIDGen.randomUUID[F]
      name = extension.map(ext => s"$id.$ext").getOrElse(id.toString)
      _ <- toInputStreamResource(stream).use(uploadObject(_, name, contentType))
    yield URI.create(s"$publicUrl$bucketName/$name")

  /** Uploads object received from stream to MinIO.
   *  @param is input stream with object.
   *  @param name desired name.
   *  @param contentType object content type.
   */
  private def uploadObject(
      is: InputStream,
      name: String,
      contentType: Option[NonEmptyString],
  ): F[Unit] =
    val putArgs = PutObjectArgs.builder
      .contentType(contentType.getOrElse(defaultContentType))
      .bucket(bucketName)
      .`object`(name)
      .stream(is, -1, partSize)
      .build
    for
      _ <- info"Uploading object with name $name to bucker $bucketName."
      _ <- Sync[F]
        .blocking(client.putObject(putArgs))
        .onError(e =>
          Logger[F].error(e)("Error while uploading object to MinIO."))
    yield ()
