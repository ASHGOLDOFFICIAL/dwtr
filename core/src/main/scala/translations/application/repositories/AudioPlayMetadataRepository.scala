package org.aulune
package translations.application.repositories


import shared.pagination.{TokenDecoder, TokenEncoder}
import shared.repositories.{GenericRepository, PaginatedList}
import translations.application.repositories.AudioPlayRepository.AudioPlayToken
import translations.domain.model.audioplay.{AudioPlay, AudioPlaySeries}
import translations.domain.shared.Uuid

import java.time.Instant
import java.util.Base64
import scala.util.Try


/** Repository for [[AudioPlayMetadata]] objects.
 *  @tparam F effect type.
 */
trait AudioPlayMetadataRepository[F[_]]
    extends GenericRepository[F, AudioPlayMetadata, Uuid[AudioPlay]]
    with PaginatedList[F, AudioPlayMetadata, AudioPlayToken]:

  /** Returns audio play series for given ID if found.
   *  @param id audio play series ID.
   */
  def getSeries(id: Uuid[AudioPlaySeries]): F[Option[AudioPlaySeries]]
