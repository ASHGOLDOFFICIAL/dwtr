package org.aulune
package translations.domain.model.shared

import java.util.UUID
import scala.util.{Failure, Success, Try}


opaque type MediaResourceId = String


object MediaResourceId:
  def apply(uuid: UUID): MediaResourceId = uuid.toString

  def unsafeApply(uuid: String): MediaResourceId = uuid

  extension (id: MediaResourceId) def string: String = id
