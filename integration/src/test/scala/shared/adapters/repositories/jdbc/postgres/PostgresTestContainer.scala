package org.aulune
package shared.adapters.repositories.jdbc.postgres


import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForEach
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.{Assertion, ParallelTestExecution}


trait PostgresTestContainer extends AsyncFreeSpec with TestContainerForEach:
  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def()

  type Init[A] = Transactor[IO] => IO[A]
  type TestCase[A] = A => IO[Assertion]

  /** @param init function that returns a service given [[Transactor]].
   *  @tparam A service type.
   *  @return function that returns [[Assertion]] from after performing actions
   *    with service of type [[A]].
   */
  def makeStand[A](init: Init[A]): TestCase[A] => IO[Assertion] =
    (app: TestCase[A]) =>
      withContainers { container =>
        val config = new HikariConfig()
        config.setDriverClassName(container.driverClassName)
        config.setJdbcUrl(container.jdbcUrl)
        config.setUsername(container.username)
        config.setPassword(container.password)
        config.setMaximumPoolSize(1)
        val transactor = HikariTransactor.fromHikariConfig[IO](config, None)

        transactor.use { t =>
          for
            service <- init(t)
            result <- app(service)
          yield result
        }
      }
