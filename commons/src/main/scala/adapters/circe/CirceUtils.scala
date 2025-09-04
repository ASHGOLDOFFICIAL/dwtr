package org.aulune.commons
package adapters.circe

import io.circe.generic.extras.Configuration


/** Circe configuration for API. */
object CirceUtils:
  given config: Configuration = Configuration.default.withSnakeCaseMemberNames
