package org.aulune.commons
package utils.imaging


import cats.data.EitherT
import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.given
import fs2.Stream
import fs2.io.toInputStreamResource

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import scala.util.{Failure, Success, Try}


/** Converts images to different formats. */
trait ImageConverter[F[_]]:
  /** Converts image received in given stream to a new format.
   *  @param stream stream with image.
   *  @param format desired format.
   *  @param size optional size (width, height) of image after conversion. If
   *    `None`, then current size will be used.
   *  @return converted image as an array of bytes, or error.
   */
  def convert(
      stream: Stream[F, Byte],
      format: ImageFormat,
      size: Option[(Int, Int)] = None,
  ): F[Either[ImageConversionError, IArray[Byte]]]


/** Object with tools to covert images. */
object ImageConverter:
  /** Default implementation of converter via awt and ImageIO.
   *  @tparam F effect type.
   */
  def apply[F[_]: Async]: ImageConverter[F] =
    (stream: Stream[F, Byte], format: ImageFormat, size: Option[(Int, Int)]) =>
      (for
        originalImage <- EitherT(readImage(stream))
        resized <- size match
          case Some(dimensions) =>
            EitherT(resizeImage(originalImage, dimensions))
          case None => EitherT.pure[F, ImageConversionError](originalImage)
        converted <- EitherT(writeImage(resized, format))
      yield IArray.unsafeFromArray(converted)).value

  /** Reads buffered image from stream.
   *  @param stream image stream.
   *  @tparam F effect type.
   *  @return buffered image, or error.
   */
  private def readImage[F[_]: Async](
      stream: Stream[F, Byte],
  ): F[Either[ImageConversionError, BufferedImage]] = toInputStreamResource(
    stream).use { is =>
    Sync[F].blocking {
      Try(Option(ImageIO.read(is))) match
        case Failure(_)     => ImageConversionError.ReadFailure.asLeft
        case Success(value) => value match
            case Some(image) => image.asRight
            case None        => ImageConversionError.UnknownFormat.asLeft
    }
  }

  /** Returns resized version of an image.
   *  @param image image to resize.
   *  @param size desired image size.
   *  @tparam F effect type.
   *  @return resized image or error.
   */
  private def resizeImage[F[_]: Sync](
      image: Image,
      size: (Int, Int),
  ): F[Either[ImageConversionError, BufferedImage]] = Sync[F].blocking {
    val (width, height) = size
    if width <= 0 || height <= 0
    then ImageConversionError.InvalidSize.asLeft
    else
      val resized = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
      val buffered =
        new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      buffered.getGraphics.drawImage(resized, 0, 0, null)
      buffered.asRight
  }

  /** Returns image in desired format as bytes.
   *  @param image image to write.
   *  @param format desired format.
   *  @tparam F effect type.
   */
  private def writeImage[F[_]: Sync](
      image: BufferedImage,
      format: ImageFormat,
  ): F[Either[ImageConversionError, Array[Byte]]] = makeOutputStream.use { os =>
    Sync[F].blocking {
      Try(ImageIO.write(image, format.name, os)) match
        case Failure(_)       => ImageConversionError.WriteFailure.asLeft
        case Success(written) =>
          if written then os.toByteArray.asRight
          else ImageConversionError.WriteFailure.asLeft
    }
  }

  /** Makes resource with output stream.
   *  @tparam F effect type.
   */
  private def makeOutputStream[F[_]: Sync]: Resource[F, ByteArrayOutputStream] =
    Resource.fromAutoCloseable(Sync[F].delay(new ByteArrayOutputStream()))
