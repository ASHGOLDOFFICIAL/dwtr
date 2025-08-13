package org.aulune
package shared.adapters.doobie.sqlite


import shared.adapters.doobie.sqlite.ErrorCode.Constraint
import shared.errors.RepositoryError

import java.sql.SQLException


/** Transforms exception to [[RepositoryError]].
 *
 *  If exception is [[SQLException]] from SQLite, then its error code will be
 *  parsed to find most appropriate [[RepositoryError]]. In other case
 *  [[RepositoryError.StorageFailure]] will be returned.
 *  @param e exception
 */
def fromSqlite(e: Throwable): RepositoryError = e match
  case e: SQLException => e.getErrorCode match
      case Constraint.code => RepositoryError.AlreadyExists
      case _               => RepositoryError.StorageFailure
  case _ => RepositoryError.StorageFailure
