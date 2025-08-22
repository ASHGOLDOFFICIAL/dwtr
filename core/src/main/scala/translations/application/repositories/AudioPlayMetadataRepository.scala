package org.aulune
package translations.application.repositories


import shared.pagination.{TokenDecoder, TokenEncoder}
import shared.repositories.{GenericRepository, PaginatedList}
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.domain.model.audioplay.AudioPlay
import translations.domain.shared.Uuid

import java.time.Instant
import java.util.Base64
import scala.util.Try


/** Repository for [[AudioPlayMetadata]] objects.
 *  @tparam F effect type.
 */
trait AudioPlayMetadataRepository[F[_]]
    extends GenericRepository[F, AudioPlayMetadata, Uuid[AudioPlay]]
    with PaginatedList[F, AudioPlayMetadata, AudioPlayToken]
