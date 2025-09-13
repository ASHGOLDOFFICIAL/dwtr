package org.aulune.aggregator
package adapters.s3


import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import fs2.Stream
import org.aulune.commons.storages.StorageError
import org.aulune.commons.testing.MinIOTestContainer
import org.aulune.commons.types.NonEmptyString
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

  private def stand = makeStand { client =>
    CoverImageStorageImpl.build[IO](
      client = client,
      publicUrl = "http://example.org/",
      bucketName = "objects",
      partSize = CoverImageStorageImpl.MinPartSize,
    )
  }

  private val name = NonEmptyString.unsafe("test")
  private val bytes = List[Byte](1, 2, 3)
  private val stream = Stream.emits(bytes)

  "contains method " - {
    "should " - {
      "return true for existing objects" in stand { storage =>
        for
          _ <- storage.put(stream, name, None)
          exists <- storage.contains(name)
        yield exists shouldBe true
      }

      "return false for non-existent objects" in stand { storage =>
        for exists <- storage.contains(name)
        yield exists shouldBe false
      }
    }
  }

  "put method " - {
    "should " - {
      "put objects" in stand { storage =>
        for
          _ <- storage.put(stream, name, None)
          resultO <- storage.get(name)
          result <- resultO match
            case Some(s) => s.compile.toList
            case None    => fail("Expected stream.")
        yield result shouldBe bytes
      }

      "throw error when object name is taken" in stand { storage =>
        for
          _ <- storage.put(stream, name, None)
          result <- storage.put(stream, name, None).attempt
        yield result match
          case Left(err) => err shouldBe StorageError.AlreadyExists
          case Right(_)  => fail("Error was expected.")
      }
    }
  }

  "get method " - {
    "should " - {
      "return None for non-existing objects" in stand { storage =>
        for result <- storage.get(name)
        yield result shouldBe None
      }
    }
  }

  "delete method " - {
    "should " - {
      "delete objects" in stand { storage =>
        for
          _ <- storage.put(stream, name, None)
          _ <- storage.delete(name)
          result <- storage.get(name)
        yield result shouldBe None
      }

      "be idempotent" in stand { storage =>
        for
          _ <- storage.delete(name)
          _ <- storage.delete(name)
          result <- storage.get(name)
        yield result shouldBe None
      }
    }
  }
