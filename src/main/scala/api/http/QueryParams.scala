package org.aulune
package api.http

import sttp.tapir.{EndpointInput, Validator, query}

object QueryParams:
  def pagination(defaultLimit: Int, maxLimit: Int): EndpointInput[(Int, Int)] =
    query[Int]("offset")
      .description("Pagination offset")
      .default(0)
      .validate(Validator.positiveOrZero)
      .and(
        query[Int]("limit")
          .description("Pagination limit")
          .default(defaultLimit)
          .validate(Validator.inRange(0, maxLimit))
      )
