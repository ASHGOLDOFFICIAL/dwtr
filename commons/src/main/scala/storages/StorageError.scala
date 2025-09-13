package org.aulune.commons
package storages

import scala.util.control.NoStackTrace


/** Errors that can occur in repository. */
enum StorageError extends NoStackTrace:
  /** The client specified an invalid argument. */
  case InvalidArgument

  /** The identity of an entity that a client attempted to create is already
   *  taken by another entity.
   */
  case AlreadyExists

  /** Internal errors. This means that some invariants expected by the
   *  underlying system have been broken.
   *  @param cause cause of internal error.
   */
  case Internal(cause: Throwable)
