package org.aulune.aggregator
package adapters.service


import adapters.service.mappers.AudioPlayMapper
import application.AggregatorPermission.Modify
import application.AudioPlayService
import application.dto.audioplay.AudioPlayResource.CastMemberResource
import application.dto.audioplay.{
  AudioPlayResource,
  CastMemberDTO,
  CreateAudioPlayRequest,
  DeleteAudioPlayRequest,
  GetAudioPlayRequest,
  ListAudioPlaysRequest,
  ListAudioPlaysResponse,
  SearchAudioPlaysRequest,
}
import application.errors.AudioPlayServiceError.{
  AudioPlayNotFound,
  AudioPlaySeriesNotFound,
  InvalidAudioPlay,
}
import domain.model.audioplay.AudioPlay
import domain.repositories.AudioPlayRepository
import domain.repositories.AudioPlayRepository.AudioPlayCursor

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.aggregator.application.dto.audioplay.series.AudioPlaySeriesResource
import org.aulune.aggregator.domain.model.audioplay.series.AudioPlaySeries
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
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.{NonEmptyString, Uuid}
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
  private given SortableUUIDGen[IO] = makeFixedUuidGen(uuid)

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
        AggregatorConfig.PaginationParams(2, 1),
        AggregatorConfig.SearchParams(2, 1),
        mockRepo,
        mockPerson,
        mockPermissions)
      .flatMap(testCase)
  end stand

  private val audioPlay = AudioPlays.audioPlay1
    .update(externalResources = Nil)
    .getOrElse(throw new IllegalStateException())

  private val audioPlayResponse = AudioPlayResource(
    id = audioPlay.id,
    title = audioPlay.title,
    synopsis = audioPlay.synopsis,
    releaseDate = audioPlay.releaseDate,
    writers = audioPlay.writers.map(Persons.resourceById),
    cast = audioPlay.cast.map(m =>
      CastMemberResource(
        actor = Persons.resourceById(m.actor),
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
    coverUri = audioPlay.coverUri,
    externalResources = Nil,
  )
  private val newUuid = Uuid[AudioPlay](uuid)
  private val newAudioPlay = audioPlay
    .update(id = newUuid, coverUrl = None)
    .getOrElse(throw new IllegalStateException())
  private val newAudioPlayResponse = audioPlayResponse.copy(
    id = newUuid,
    coverUri = None,
  )

  "get method " - {
    val request = GetAudioPlayRequest(audioPlay.id)

    "should " - {
      "find audio plays if they're present in repository" in stand { service =>
        val _ = mockGet(audioPlay.some.pure)
        for result <- service.get(request)
        yield result shouldBe audioPlayResponse.asRight
      }

      "result in AudioPlayNotFound if audio play doesn't exist" in stand {
        service =>
          val _ = mockGet(None.pure)
          val find = service.get(request)
          assertDomainError(find)(AudioPlayNotFound)
      }

      "handle errors from repository gracefully" in stand { service =>
        val _ = mockGet(IO.raiseError(new Throwable()))
        val find = service.get(request)
        assertInternalError(find)
      }
    }
  }

  "list method " - {
    "should " - {
      "list elements" in stand { service =>
        val request = ListAudioPlaysRequest(
          pageSize = 1.some,
          pageToken = None,
        )
        val _ =
          (mockRepo.list _).expects(None, 1).returning(List(audioPlay).pure)
        for result <- service.list(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.audioPlays shouldBe List(audioPlayResponse)
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
        val response = AudioPlayMapper.toResponse(
          AudioPlays.audioPlay2,
          Persons.resourceById)
        for result <- service.list(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.audioPlays shouldBe List(response)
      }
    }
  }

  "search method " - {
    val query = NonEmptyString.unsafe("thing")
    val request = SearchAudioPlaysRequest(
      query = query,
      limit = 2.some,
    )

    "should " - {
      "return list of elements" in stand { service =>
        val elements = List(
          AudioPlays.audioPlay3,
          AudioPlays.audioPlay2,
        )
        val _ = (mockRepo.search _)
          .expects(query, 2)
          .returning(elements.pure)

        for result <- service.search(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.audioPlays shouldBe elements.map(
              AudioPlayMapper.toResponse(_, Persons.resourceById))
      }
    }
  }

  "create method " - {
    val request = CreateAudioPlayRequest(
      title = audioPlay.title,
      synopsis = audioPlay.synopsis,
      releaseDate = audioPlay.releaseDate,
      writers = audioPlay.writers,
      cast = audioPlay.cast.map(m =>
        CastMemberDTO(
          actor = m.actor,
          roles = m.roles,
          main = m.main,
        )),
      seriesId = audioPlay.series.map(_.id),
      seriesSeason = audioPlay.seriesSeason,
      seriesNumber = audioPlay.seriesNumber,
      externalResources = Nil,
    )

    "should " - {
      "allow users with permissions to create audio plays if none exist" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockGetSeries(audioPlay.series.pure)
          val _ = mockPersist(newAudioPlay.pure)
          for result <- service.create(user, request)
          yield result shouldBe newAudioPlayResponse.asRight
      }

      "result in InvalidAudioPlay when creating invalid audio play" in stand {
        service =>
          val emptyNameRequest = request.copy(title = "")
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockGetSeries(audioPlay.series.pure)
          val find = service.create(user, emptyNameRequest)
          assertDomainError(find)(InvalidAudioPlay)
      }

      "result in AudioPlaySeriesNotFound when creating audio play of non-existent series" in stand {
        service =>
          val emptyNameRequest = request.copy(title = "")
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockGetSeries(None.pure)
          val find = service.create(user, emptyNameRequest)
          assertDomainError(find)(AudioPlaySeriesNotFound)
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val find = service.create(user, request)
        assertErrorStatus(find)(PermissionDenied)
      }

      "handle exceptions from hasPermission gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
        val find = service.create(user, request)
        assertInternalError(find)
      }

      "handle exceptions from persist gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockGetSeries(audioPlay.series.pure)
        val _ = mockPersist(IO.raiseError(new Throwable()))
        val find = service.create(user, request)
        assertInternalError(find)
      }
    }
  }

  "delete method " - {
    val request = DeleteAudioPlayRequest(audioPlay.id)

    "should " - {
      "allow users with permissions to delete existing audio plays" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockDelete(().pure)
          for result <- service.delete(user, request)
          yield result shouldBe ().asRight
      }

      "result in PermissionDenied for unauthorized users" in stand { service =>
        val _ = mockHasPermission(Modify, false.asRight.pure)
        val delete = service.delete(user, request)
        assertErrorStatus(delete)(PermissionDenied)
      }

      "handle exceptions from hasPermission gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
        val delete = service.delete(user, request)
        assertInternalError(delete)
      }

      "handle exceptions from delete gracefully" in stand { service =>
        val _ = mockHasPermission(Modify, true.asRight.pure)
        val _ = mockDelete(IO.raiseError(new Throwable()))
        val delete = service.delete(user, request)
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
