package org.aulune.aggregator
package adapters.s3


import adapters.jdbc.postgres.AudioPlayRepositoryImpl

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2.Stream
import org.aulune.commons.testing.MinIOTestContainer
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory


/** Tests for [[CoverImageStorageImpl]]. */
final class CoverImageStorageImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with MinIOTestContainer:

  private given LoggerFactory[IO] = Slf4jFactory.create

  private def stand = makeStand(client =>
    CoverImageStorageImpl.build[IO](
      client = client,
      publicUrl = "http://example.org/",
      bucketName = "objects",
      partSize = CoverImageStorageImpl.MinPartSize,
    ))

  "upload method " - {
    "should " - {
      "upload object" in stand { uploader =>
        val stream = Stream.emits(List[Byte](1, 2, 3))
        for uri <-
            uploader.issueURI(stream, contentType = None, extension = None)
        yield ()
      }
    }
  }
