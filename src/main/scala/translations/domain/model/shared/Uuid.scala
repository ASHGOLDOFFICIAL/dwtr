package org.aulune
package translations.domain.model.shared


import java.util.UUID
import scala.util.Try


/** UUID for given [[A]]. */
opaque type Uuid[A] <: UUID = UUID


object Uuid:
  /** Returns [[Uuid]] from given UUID.
   *  @param uuid UUID.
   */
  def apply[A](uuid: UUID): Uuid[A] = uuid

  /** Validates string to be valid UUID.
   *  @param uuid UUID.
   *  @return [[Uuid]] if string is valid UUID.
   */
  def apply[A](uuid: String): Option[Uuid[A]] =
    Try(UUID.fromString(uuid)).toOption
