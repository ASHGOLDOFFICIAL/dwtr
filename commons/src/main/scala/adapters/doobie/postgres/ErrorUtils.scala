package org.aulune.commons.adapters.doobie.postgres


import cats.MonadThrow
import cats.syntax.all.given
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  AlreadyExists,
  FailedPrecondition,
  Internal,
  InvalidArgument,
}

import java.sql.SQLException


/** Error utils to use across different PostgreSQL repositories. */
object ErrorUtils:
  /** Checks if number is positive, otherwise throws [[FailedPrecondition]].
   *  @param updatedRows number of updated rows.
   *  @tparam F effect type.
   */
  def checkIfUpdated[F[_]: MonadThrow](updatedRows: Int): F[Unit] =
    MonadThrow[F].raiseWhen(updatedRows <= 0)(FailedPrecondition)

  /** Raises [[InvalidArgument]] when given non-positive number.
   *  @param int number to validate.
   *  @tparam F effect type.
   */
  def checkIfPositive[F[_]: MonadThrow](int: Int): F[Unit] =
    MonadThrow[F].raiseWhen(int <= 0)(InvalidArgument)

  /** Converts unique violation exceptions to [[AlreadyExists]].
   *  @tparam A needed return type.
   */
  def toAlreadyExists[F[_]: MonadThrow, A]: PartialFunction[Throwable, F[A]] =
    case e: SQLException if e.getSQLState == UNIQUE_VIOLATION.value =>
      AlreadyExists.raiseError

  /** Packs all errors except [[RepositoryError]]s inside [[Internal]].
   *  @param e error to pack.
   *  @tparam A needed return type.
   */
  def toInternalError[F[_]: MonadThrow, A](e: Throwable): F[A] = e match
    case re: RepositoryError => re.raiseError
    case t                   => Internal(t).raiseError
