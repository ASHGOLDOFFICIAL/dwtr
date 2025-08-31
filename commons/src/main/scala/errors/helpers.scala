package org.aulune.commons
package errors


import repositories.RepositoryError


/** Converts [[RepositoryError]] to corresponding [[ErrorStatus]].
 *
 *  @param err repository error.
 *  @return corresponding application error.
 */
def toApplicationError(err: Throwable): ErrorStatus = err match
  case e: ErrorStatus    => e
  case RepositoryError.AlreadyExists => ErrorStatus.AlreadyExists
  case RepositoryError.FailedPrecondition =>
    ErrorStatus.FailedPrecondition
  case RepositoryError.Internal => ErrorStatus.Internal
