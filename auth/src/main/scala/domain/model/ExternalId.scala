package org.aulune.auth
package domain.model

/** Non-empty user's ID in external service. */
opaque type ExternalId <: String = String


object ExternalId:
  /** Returns [[ExternalId]] if given argument is valid.
   *  @param id user ID in external service.
   */
  def apply(id: String): Option[ExternalId] = Option.when(id.nonEmpty)(id)

  /** Unsafe constructor to use within always-valid boundary.
   *  @param id user ID in external service.
   *  @throws IllegalArgumentException if invalid arguments are given.
   */
  def unsafe(id: String): ExternalId = ExternalId(id) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
