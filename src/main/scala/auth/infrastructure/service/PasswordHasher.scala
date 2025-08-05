package org.aulune
package auth.infrastructure.service


import cats.effect.Sync
import cats.syntax.all.*
import de.mkammerer.argon2.{Argon2, Argon2Factory}


class PasswordHasher[F[_]: Sync](argon2: Argon2):
  /* array wiping is done inside Argon2, but better safe than sorry. */
  def hashPassword(password: String): F[String] =
    val passwordChars = password.toCharArray
    Sync[F].bracket(passwordChars.pure[F])(chars =>
      Sync[F].catchNonFatal(argon2.hash(10, 65536, 1, chars)))(chars =>
      Sync[F].delay(argon2.wipeArray(chars)))

  def validatePassword(password: String, hashed: String): F[Boolean] =
    val passwordChars = password.toCharArray
    Sync[F].bracket(passwordChars.pure[F])(chars =>
      Sync[F].catchNonFatal(argon2.verify(hashed, chars)))(chars =>
      Sync[F].delay(argon2.wipeArray(chars)))


object PasswordHasher:
  def build[F[_]: Sync]: F[PasswordHasher[F]] = Sync[F]
    .delay(Argon2Factory.create())
    .map(argon2 => new PasswordHasher[F](argon2))
