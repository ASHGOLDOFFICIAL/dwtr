package org.aulune.aggregator
package adapters.service


import adapters.service.mappers.AudioPlayMapper
import application.AggregatorPermission.Modify
import application.AudioPlayService
import application.dto.audioplay.{
  AudioPlayResource,
  AudioPlaySeriesResource,
  CastMemberDto,
  CreateAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
}
import application.errors.AudioPlayServiceError.{
  AudioPlayNotFound,
  AudioPlaySeriesNotFound,
  InvalidAudioPlay,
}
import application.repositories.AudioPlayRepository
import application.repositories.AudioPlayRepository.AudioPlayCursor
import domain.model.audioplay.{AudioPlay, AudioPlaySeries}
import testing.{AudioPlays, Persons}

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.errors.ErrorStatus.PermissionDenied
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.{
  Permission,
  PermissionClientService,
}
import org.aulune.commons.testing.ErrorAssertions.{
  assertDomainError,
  assertErrorStatus,
  assertInternalError,
}
import org.aulune.commons.testing.instances.UUIDGenInstances.makeFixedUuidGen
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID


/** Tests for [[AudioPlayServiceImpl]]. */
final class AudioPlayServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private given LoggerFactory[IO] = Slf4jFactory.create

  private val mockRepo = mock[AudioPlayRepository[IO]]
  private val mockPerson = Persons.service[IO]
  private val mockPermissions = mock[PermissionClientService[IO]]

  private val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private given UUIDGen[IO] = makeFixedUuidGen(uuid)

  private val user = User(
    id = UUID.fromString("f04eb510-229c-4cdd-bd7b-9691c3b28ae1"),
    username = "username",
  )

  private def stand(
      testCase: AudioPlayService[IO] => IO[Assertion],
  ): IO[Assertion] =
    val _ = (mockPermissions.registerPermission _)
      .expects(*)
      .returning(().asRight.pure)
      .anyNumberOfTimes()
    AudioPlayServiceImpl
      .build(
        AggregatorConfig.Pagination(2, 1),
        mockRepo,
        mockPerson,
        mockPermissions)
      .flatMap(testCase)
  end stand

  private val audioPlay =
    testing.AudioPlays.audioPlay1.update(externalResources = Nil).toOption.get

  private val audioPlayResponse = AudioPlayResource(
    id = audioPlay.id,
    title = audioPlay.title,
    synopsis = audioPlay.synopsis,
    releaseDate = audioPlay.releaseDate,
    writers = audioPlay.writers,
    cast = audioPlay.cast.map(m =>
      CastMemberDto(
        actor = m.actor,
        roles = m.roles,
        main = m.main,
      )),
    series = audioPlay.series.map(s =>
      AudioPlaySeriesResource(
        id = s.id,
        name = s.name,
      )),
    seriesSeason = audioPlay.seriesSeason,
    seriesNumber = audioPlay.seriesNumber,
    coverUrl = audioPlay.coverUrl,
    externalResources = Nil,
  )
  private val createRequest = CreateAudioPlayRequest(
    title = audioPlay.title,
    synopsis = audioPlay.synopsis,
    releaseDate = audioPlay.releaseDate,
    writers = audioPlay.writers,
    cast = audioPlay.cast.map(m =>
      CastMemberDto(
        actor = m.actor,
        roles = m.roles,
        main = m.main,
      )),
    seriesId = audioPlay.series.map(_.id),
    seriesSeason = audioPlay.seriesSeason,
    seriesNumber = audioPlay.seriesNumber,
    externalResources = Nil,
  )
  private val newUuid = Uuid[AudioPlay](uuid)
  private val newAudioPlay =
    audioPlay.update(id = newUuid, coverUrl = None).toOption.get
  private val newAudioPlayResponse = audioPlayResponse.copy(
    id = newUuid,
    coverUrl = None,
  )

  "findById method " - {
    "should " - {
      "find audio plays if they're present in repository" in stand { service =>
        val _ = mockGet(audioPlay.some.pure)
        for result <- service.findById(audioPlay.id)
        yield result shouldBe audioPlayResponse.asRight
      }

      "result in AudioPlayNotFound if audio play doesn't exist" in stand {
        service =>
          val _ = mockGet(None.pure)
          val find = service.findById(audioPlay.id)
          assertDomainError(find)(AudioPlayNotFound)
      }

      "handle errors from repository gracefully" in stand { service =>
        val _ = mockGet(IO.raiseError(new Throwable()))
        val find = service.findById(audioPlay.id)
        assertInternalError(find)
      }
    }
  }

  "listAll method " - {
    "should " - {
      "list elements" in stand { service =>
        val request = ListAudioPlaysRequest(
          pageSize = 1.some,
          pageToken = None,
        )
        val _ =
          (mockRepo.list _).expects(None, 1).returning(List(audioPlay).pure)
        for result <- service.listAll(request)
        yield result shouldBe ListAudioPlaysResponse(
          audioPlays = List(audioPlayResponse),
          nextPageToken =
            "M2Y4YTIwMmUtNjA5ZC00OWIyLWE2NDMtOTA3YjM0MWNlYTY2".some,
        ).asRight
      }

      "return next page when asked" in stand { service =>
        val request = ListAudioPlaysRequest(
          pageSize = 1.some,
          pageToken = "M2Y4YTIwMmUtNjA5ZC00OWIyLWE2NDMtOTA3YjM0MWNlYTY2".some,
        )
        val cursor = AudioPlayCursor(audioPlay.id).some
        val _ = (mockRepo.list _)
          .expects(cursor, 1)
          .returning(List(AudioPlays.audioPlay2).pure)

        for result <- service.listAll(request)
        yield result shouldBe ListAudioPlaysResponse(
          audioPlays = List(AudioPlayMapper.toResponse(AudioPlays.audioPlay2)),
          nextPageToken =
            "MDE5OGQyMTctMmU5NS03Yjk0LTgwYTctYTc2MjU4OWRlNTA2".some,
        ).asRight
      }
    }
  }

  "create method " - {
    "should " - {
      "allow users with permissions to create audio plays if none exist" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockGetSeries(audioPlay.series.pure)
          val _ = mockPersist(newAudioPlay.pure)
          for result <- service.create(user, createRequest)
          yield result shouldBe newAudioPlayResponse.asRight
      }

      "result in InvalidAudioPlay when creating invalid audio play" in stand {
        service =>
          val emptyNameRequest = createRequest.copy(title = "")
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockGetSeries(audioPlay.series.pure)
          val find = service.create(user, emptyNameRequest)
          assertDomainError(find)(InvalidAudioPlay)
      }

      "result in AudioPlaySeriesNotFound when creating audio play of non-existent series" in stand {
        service =>
          val emptyNameRequest = createRequest.copy(title = "")
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockGetSeries(None.pure)
          val find = service.create(user, emptyNameRequest)
          assertDomainError(find)(AudioPlaySeriesNotFound)
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val find = service.create(user, createRequest)
        assertErrorStatus(find)(PermissionDenied)
      }

      "handle exceptions from hasPermission gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
        val find = service.create(user, createRequest)
        assertInternalError(find)
      }

      "handle exceptions from persist gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockGetSeries(audioPlay.series.pure)
        val _ = mockPersist(IO.raiseError(new Throwable()))
        val find = service.create(user, createRequest)
        assertInternalError(find)
      }
    }
  }

  "delete method " - {
    "should " - {
      "allow users with permissions to delete existing audio plays" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockDelete(().pure)
          for result <- service.delete(user, audioPlay.id)
          yield result shouldBe ().asRight
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val delete = service.delete(user, audioPlay.id)
        assertErrorStatus(delete)(PermissionDenied)
      }

      "handle exceptions from hasPermission gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
        val delete = service.delete(user, audioPlay.id)
        assertInternalError(delete)
      }

      "handle exceptions from delete gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockDelete(IO.raiseError(new Throwable()))
        val delete = service.delete(user, audioPlay.id)
        assertInternalError(delete)
      }
    }
  }

  private def mockPersist(returning: IO[AudioPlay]) =
    (mockRepo.persist _).expects(newAudioPlay).returning(returning)

  private def mockGet(returning: IO[Option[AudioPlay]]) =
    (mockRepo.get _).expects(audioPlay.id).returning(returning)

  private def mockDelete(returning: IO[Unit]) =
    (mockRepo.delete _).expects(audioPlay.id).returning(returning)

  private def mockGetSeries(returning: IO[Option[AudioPlaySeries]]) =
    (mockRepo.getSeries _).expects(audioPlay.series.get.id).returning(returning)

  private def mockHasPermission(
      permission: Permission,
      returning: IO[Either[ErrorResponse, Boolean]],
  ) = (mockPermissions.hasPermission _)
    .expects(user, permission)
    .returning(returning)

end AudioPlayServiceImplTest
