package org.aulune
package auth.adapters.service

import auth.domain.service.PasswordHashingService

import cats.effect.{Resource, Sync}
import cats.syntax.all.*
import de.mkammerer.argon2.{Argon2, Argon2Factory}


/** Password hashing service with Argon2i as its hashing algorithm. */
object Argon2iPasswordHashingService:
  /** Builds the service.
   *  @tparam F effect type.
   */
  def build[F[_]: Sync]: F[PasswordHashingService[F]] = Sync[F]
    .delay(Argon2Factory.create())
    .map(argon2 => new Argon2iPasswordHashingService[F](argon2))


private final class Argon2iPasswordHashingService[F[_]: Sync](argon2i: Argon2)
    extends PasswordHashingService[F]:
  override def hashPassword(password: String): F[String] =
    passwordResource(password)
      .use(chars => Sync[F].blocking(argon2i.hash(10, 65536, 1, chars)))

  override def verifyPassword(password: String, hashed: String): F[Boolean] =
    passwordResource(password).use { chars =>
      Sync[F].blocking(argon2i.verify(hashed, chars))
    }

  /** Manages the password as a char array in a Resource, ensuring the array is
   *  wiped.
   *  @param password plain password string.
   *  @return password char array wrapped in a Resource.
   *  @note Argon2 does array wiping automatically, but this way we ensure that
   *    it will be done even if operation will be canceled.
   */
  private def passwordResource(password: String) = Resource
    .make(password.toCharArray.pure[F]) { chars =>
      Sync[F].delay(argon2i.wipeArray(chars))
    }
