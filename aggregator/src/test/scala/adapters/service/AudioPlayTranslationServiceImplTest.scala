package org.aulune.aggregator
package adapters.service


import adapters.service.mappers.{
  AudioPlayTranslationMapper,
  AudioPlayTranslationTypeMapper,
  LanguageMapper,
}
import application.AggregatorPermission.Modify
import application.AudioPlayTranslationService
import application.dto.audioplay.translation.{
  AudioPlayTranslationResource,
  CreateAudioPlayTranslationRequest,
  DeleteAudioPlayTranslationRequest,
  GetAudioPlayTranslationRequest,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
}
import application.errors.TranslationServiceError.{
  InvalidTranslation,
  OriginalNotFound,
  TranslationNotFound,
}
import domain.repositories.AudioPlayTranslationRepository
import domain.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.aggregator.domain.model.audioplay.translation.AudioPlayTranslation
import org.aulune.aggregator.domain.model.shared.TranslatedTitle
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
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID


/** Tests for [[AudioPlayTranslationServiceImpl]]. */
final class AudioPlayTranslationServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  private given LoggerFactory[IO] = Slf4jFactory.create

  private val mockRepo = mock[AudioPlayTranslationRepository[IO]]
  private val mockAudio = AudioPlays.service[IO]
  private val mockPermissions = mock[PermissionClientService[IO]]

  private val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private given SortableUUIDGen[IO] = makeFixedUuidGen(uuid)

  private val user = User(
    id = UUID.fromString("f04eb510-229c-4cdd-bd7b-9691c3b28ae1"),
    username = "username",
  )

  private def stand(
      testCase: AudioPlayTranslationService[IO] => IO[Assertion],
  ): IO[Assertion] =
    val _ = (mockPermissions.registerPermission _)
      .expects(*)
      .returning(().asRight.pure)
      .anyNumberOfTimes()
    AudioPlayTranslationServiceImpl
      .build(
        AggregatorConfig.PaginationParams(2, 1),
        mockRepo,
        mockAudio,
        mockPermissions)
      .flatMap(testCase)
  end stand

  private val translation = AudioPlayTranslations.translation1
  private val translationResponse = AudioPlayTranslationResource(
    originalId = translation.originalId,
    id = translation.id,
    title = translation.title,
    translationType = AudioPlayTranslationTypeMapper
      .fromDomain(translation.translationType),
    language = LanguageMapper.fromDomain(translation.language),
    links = translation.links.toList,
  )

  private val newUuid = Uuid[AudioPlayTranslation](uuid)
  private val newTranslation = translation
    .update(id = newUuid, title = TranslatedTitle.unsafe("Updated"))
    .getOrElse(throw new IllegalStateException())
  private val newTranslationResponse = translationResponse.copy(
    id = newUuid,
    title = newTranslation.title,
  )

  "get method " - {
    val request = GetAudioPlayTranslationRequest(translation.id)

    "should " - {
      "find translations if they're present in repository" in stand { service =>
        val _ = mockGet(translation.some.pure)
        for result <- service.get(request)
        yield result shouldBe translationResponse.asRight
      }

      "result in TranslationNotFound if translation doesn't exist" in stand {
        service =>
          val _ = mockGet(None.pure)
          val find = service.get(request)
          assertDomainError(find)(TranslationNotFound)
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
        val request = ListAudioPlayTranslationsRequest(
          pageSize = 1.some,
          pageToken = None,
        )
        val _ = (mockRepo.list _)
          .expects(None, 1)
          .returning(List(translation).pure)

        for result <- service.list(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) =>
            list.translations shouldBe List(translationResponse)
      }

      "return next page when asked" in stand { service =>
        val request = ListAudioPlayTranslationsRequest(
          pageSize = 1.some,
          pageToken = "NjVlZTBlNjItNDc4MC00Nzc3LWEyOTYtM2VmMGRhOWJlN2U4".some,
        )
        val cursor = AudioPlayTranslationCursor(translation.id).some
        val _ = (mockRepo.list _)
          .expects(cursor, 1)
          .returning(List(AudioPlayTranslations.translation2).pure)
        val response = AudioPlayTranslationMapper.toResponse(
          AudioPlayTranslations.translation2)

        for result <- service.list(request)
        yield result match
          case Left(_)     => fail("Error was not expected")
          case Right(list) => list.translations shouldBe List(response)
      }
    }
  }

  "create method " - {
    val request = CreateAudioPlayTranslationRequest(
      originalId = newTranslation.originalId,
      title = newTranslation.title,
      translationType = AudioPlayTranslationTypeMapper
        .fromDomain(newTranslation.translationType),
      language = LanguageMapper.fromDomain(newTranslation.language),
      links = newTranslation.links.toList,
    )

    "should " - {
      "allow users with permissions to create translations if none exist" in stand {
        service =>
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val _ = mockPersist(newTranslation.pure)
          for result <- service.create(user, request)
          yield result shouldBe newTranslationResponse.asRight
      }

      "result in InvalidTranslation when creating invalid translation" in stand {
        service =>
          val emptyNameRequest = request.copy(title = "")
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val find = service.create(user, emptyNameRequest)
          assertDomainError(find)(InvalidTranslation)
      }

      "result in OriginalNotFound when original audio play doesn't exist" in stand {
        service =>
          val invalidRequest = request.copy(originalId = uuid)
          val _ = mockHasPermission(Modify, true.asRight.pure)
          val find = service.create(user, invalidRequest)
          assertDomainError(find)(OriginalNotFound)
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
        val _ = mockPersist(IO.raiseError(new Throwable()))
        val find = service.create(user, request)
        assertInternalError(find)
      }
    }
  }

  "delete method " - {
    val request = DeleteAudioPlayTranslationRequest(translation.id)

    "should " - {
      "allow users with permissions to delete existing translations" in stand {
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

  private def mockPersist(returning: IO[AudioPlayTranslation]) =
    (mockRepo.persist _).expects(newTranslation).returning(returning)

  private def mockGet(returning: IO[Option[AudioPlayTranslation]]) =
    (mockRepo.get _).expects(translation.id).returning(returning)

  private def mockDelete(returning: IO[Unit]) =
    (mockRepo.delete _).expects(translation.id).returning(returning)

  private def mockHasPermission(
      permission: Permission,
      returning: IO[Either[ErrorResponse, Boolean]],
  ) = (mockPermissions.hasPermission _)
    .expects(user, permission)
    .returning(returning)

end AudioPlayTranslationServiceImplTest
