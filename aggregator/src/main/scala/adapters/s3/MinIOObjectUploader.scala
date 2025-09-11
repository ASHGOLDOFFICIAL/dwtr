package org.aulune.aggregator
package adapters.s3


import application.ObjectUploader

import cats.MonadThrow
import cats.effect.std.UUIDGen
import cats.effect.{Resource, Sync}
import cats.syntax.all.given
import io.minio.{BucketExistsArgs, MinioClient, PutObjectArgs}
import org.aulune.commons.types.NonEmptyString
import org.typelevel.log4cats.syntax.given
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.io.ByteArrayInputStream
import java.net.URI


object MinIOObjectUploader:
  /** Builds MinIO file uploader.
   *  @param client MinIO client.
   *  @param endpoint MinIO endpoint to make URIs.
   *  @param bucketName name of bucket to use. Should already exist.
   *  @tparam F effect type.
   *  @throws IllegalStateException if bucket doesn't exist.
   */
  def build[F[_]: Sync: UUIDGen: LoggerFactory](
      client: MinioClient,
      endpoint: String,
      bucketName: String,
  ): F[ObjectUploader[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    for
      exist <- checkIfBucketExists(client, bucketName)
      _ <- MonadThrow[F]
        .raiseUnless(exist)(new IllegalStateException())
        .onError(_ => error"Bucket $bucketName doesn't exists.")
      normalized = if endpoint.endsWith("/") then endpoint else endpoint + "/"
    yield MinIOObjectUploader(client, normalized, bucketName)

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

end MinIOObjectUploader


/** Object uploader implementation via MinIO.
 *  @param client MinIO client.
 *  @param baseUrl MinIO endpoint, should end with '/'.
 *  @param bucketName MinIO bucket to use.
 *  @tparam F effect type.
 */
private final class MinIOObjectUploader[F[_]: Sync: UUIDGen: LoggerFactory](
    client: MinioClient,
    baseUrl: String,
    bucketName: String,
) extends ObjectUploader[F]:
  private given Logger[F] = LoggerFactory[F].getLogger

  override def upload(
      bytes: Array[Byte],
      extension: Option[NonEmptyString],
  ): F[URI] = Resource
    .fromAutoCloseable(Sync[F].blocking(new ByteArrayInputStream(bytes)))
    .use { stream =>
      for
        id <- UUIDGen.randomUUID
        name = extension.map(ext => s"$id.$ext").getOrElse(s"id")
        putArgs = PutObjectArgs.builder
          .bucket(bucketName)
          .`object`(name)
          .stream(stream, stream.available(), -1)
          .build
        _ <- info"Uploading object with name $name to bucker $bucketName."
        _ <- Sync[F].blocking(client.putObject(putArgs))
      yield URI.create(s"$baseUrl$bucketName/$name")
    }
