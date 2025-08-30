package org.aulune.auth
package domain.service


/** Manages password hashing and verification.
 *  @tparam F effect type.
 */
trait PasswordHashingService[F[_]]:
  /** Hashes the provided plain-text password.
   *
   *  @param password plain password.
   */
  def hashPassword(password: String): F[String]

  /** Verifies a plain-text password against a hashed password.
   *
   *  @param password plain password.
   *  @param hashed hashed password.
   *  @return `true` if passwords match, otherwise `false`.
   */
  def verifyPassword(password: String, hashed: String): F[Boolean]
