package org.aulune
package shared.adapters.repositories.jdbc.sqlite


import cats.effect.{IO, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor
import org.scalatest.compatible.Assertion


/** In-memory SQLite database for testing. */
object SqliteStand:
  /** @param init function that returns a service given [[Transactor]].
   *  @tparam A service type.
   *  @return function that returns [[Assertion]] from function on [[A]].
   */
  def apply[A](
      init: Transactor[IO] => IO[A],
  ): (A => IO[Assertion]) => IO[Assertion] =
    (makeAssertions: A => IO[Assertion]) =>
      makeTransactor.use { transactor =>
        for
          service <- init(transactor)
          result <- makeAssertions(service)
        yield result
      }

  /** Creates [[Transactor]] for in-memory SQLite. */
  private def makeTransactor: Resource[IO, Transactor[IO]] =
    val config = new HikariConfig()
    config.setDriverClassName(classOf[org.sqlite.JDBC].getName)
    config.setJdbcUrl("jdbc:sqlite::memory:")
    config.setMaximumPoolSize(1)
    HikariTransactor.fromHikariConfig[IO](config, None)
