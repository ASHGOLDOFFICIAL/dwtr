package org.aulune
package commons.http.circe

import io.circe.generic.extras.Configuration


/** Circe configuration for API. */
object CirceConfiguration:
  given config: Configuration = Configuration.default.withSnakeCaseMemberNames
