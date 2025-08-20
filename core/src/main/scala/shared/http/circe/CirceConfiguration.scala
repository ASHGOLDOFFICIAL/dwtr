package org.aulune
package shared.http.circe

import io.circe.generic.extras.Configuration

object CirceConfiguration:
  given config: Configuration = Configuration.default.withSnakeCaseMemberNames
