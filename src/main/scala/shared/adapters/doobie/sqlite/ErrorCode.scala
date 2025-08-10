package org.aulune
package shared.adapters.doobie.sqlite


/** SQLite error codes.
 *  @param code error code as Int.
 *  @see [[https://www.sqlite.org/rescode.html SQLite documentation]]
 */
enum ErrorCode(val code: Int):
  /** (19) SQLITE_CONSTRAINT */
  case Constraint extends ErrorCode(19)
