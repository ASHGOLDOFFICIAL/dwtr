package org.aulune
package commons.instances

import cats.Monad
import cats.effect.kernel.Clock
import cats.effect.std.{SecureRandom, UUIDGen}
import cats.syntax.all.*

import java.nio.ByteBuffer
import java.util.UUID


/** UUIDv7 implementation.
 *  @see
 *    [[https://www.rfc-editor.org/rfc/rfc9562.html#name-uuid-version-7 RFC 9562]].
 */
object UUIDv7Gen:
  private def randomBytes[F[_]: Monad: Clock: SecureRandom]: F[Array[Byte]] =
    for
      bytes <- SecureRandom[F].nextBytes(16) // creates 128-bit array.
      time <- Clock[F].realTime

      timestamp =
        ByteBuffer.allocate(8).putLong(time.toMillis) // allocates 64 bits.

      // copies 48 lest significant bits to the beginning of bytes.
      _ = System.arraycopy(timestamp.array, 2, bytes, 0, 6)

      // clears bits 48-51, then puts 7 there (version).
      _ = bytes(6) = ((bytes(6) & 0x0f) | 0x70).toByte

      // clears bits 64-65, then puts 0b10 there (variant field).
      _ = bytes(8) = ((bytes(8) & 0x3f) | 0x80).toByte
    yield bytes

  given uuidv7Instance[F[_]: Monad: SecureRandom: Clock]: UUIDGen[F] =
    new UUIDGen[F]:
      def randomUUID: F[UUID] =
        for
          value <- randomBytes[F]
          buf = ByteBuffer.wrap(value)
          high = buf.getLong
          low = buf.getLong
        yield new UUID(high, low)
