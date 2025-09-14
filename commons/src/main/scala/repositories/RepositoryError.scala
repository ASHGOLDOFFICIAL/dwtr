package org.aulune.commons
package repositories

import scala.util.control.NoStackTrace


/** Errors that can occur in repository. */
enum RepositoryError extends NoStackTrace:
  /** The client specified an invalid argument. */
  case InvalidArgument

  case AlreadyExists

  /** Client attempted to create or update an entity that violates some unique
   *  constraint.
   *  @param constraint violated constraint.
   *  @tparam A type of constraints.
   */
  case ConstraintViolation[A](constraint: A)

  /** The operation was rejected because the system is not in a state required
   *  for the operation's execution. For example, updating entity that doesn't
   *  exist.
   */
  case FailedPrecondition

  /** Internal errors. This means that some invariants expected by the
   *  underlying system have been broken.
   *  @param cause cause of internal error.
   */
  case Internal(cause: Throwable)
