package org.aulune.commons.adapters.doobie.postgres


import cats.MonadThrow
import cats.syntax.all.given
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.repositories.RepositoryError.{
  ConstraintViolation,
  FailedPrecondition,
  Internal,
  InvalidArgument,
}
import org.postgresql.util.PSQLException

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

  /** If error contains information about violation for which exists a value in
   *  given map, then [[ConstraintViolation]] with an associated value will be
   *  thrown. In other cases exception will be rethrown.
   *
   *  @param map map between string describing violation and an associated type.
   *  @tparam F effect type.
   *  @tparam A needed return type.
   *  @tparam C constraint type.
   */
  def makeConstraintViolationConverter[F[_]: MonadThrow, A, C](
      map: Map[String, C],
  ): PartialFunction[Throwable, F[A]] =
    case e: PSQLException if e.getServerErrorMessage == null =>
      e.raiseError[F, A]
    case e: PSQLException if e.getServerErrorMessage.getConstraint == null =>
      e.raiseError[F, A]
    case e: PSQLException =>
      val constraint = e.getServerErrorMessage.getConstraint
      map.get(constraint) match
        case Some(value) => ConstraintViolation[C](value).raiseError[F, A]
        case None        => e.raiseError[F, A]

  /** Packs all errors except [[RepositoryError]]s inside [[Internal]].
   *  @param e error to pack.
   *  @tparam A needed return type.
   */
  def toInternalError[F[_]: MonadThrow, A](e: Throwable): F[A] = e match
    case re: RepositoryError => re.raiseError
    case t                   => Internal(t).raiseError
