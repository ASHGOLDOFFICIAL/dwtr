package org.aulune.commons
package testing


import cats.effect.IO
import com.dimafeng.testcontainers.MinIOContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForEach
import io.minio.MinioClient
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.testcontainers.utility.DockerImageName


/** Test container with MinIO. */
trait MinIOTestContainer extends AsyncFreeSpec with TestContainerForEach:
  private val minioPort = 9000
  private val accessKey: String = "minioadmin"
  private val secretKey: String = "minioadmin"

  override val containerDef: MinIOContainer.Def = MinIOContainer.Def(
    dockerImageName = DockerImageName.parse("minio/minio:latest"),
    userName = accessKey,
    password = secretKey,
  )

  private type Init[A] = MinioClient => IO[A]
  private type TestCase[A] = A => IO[Assertion]

  /** @param init function that returns a service given [[MinIOClient]].
   *  @tparam A service type.
   *  @return function that returns [[Assertion]] from after performing actions
   *    with service of type [[A]].
   */
  protected def makeStand[A](init: Init[A]): TestCase[A] => IO[Assertion] =
    (app: TestCase[A]) =>
      withContainers { container =>
        val host = container.host
        val port = container.mappedPort(minioPort)
        val endpoint = s"http://$host:$port"

        val client = MinioClient
          .builder()
          .endpoint(endpoint)
          .credentials(accessKey, secretKey)
          .build()

        for
          service <- init(client)
          result <- app(service)
        yield result
      }
