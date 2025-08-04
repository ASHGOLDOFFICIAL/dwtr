package org.aulune
package api.http

import sttp.tapir.{EndpointInput, Validator, query}


object QueryParams:
  def pagination(
      defaultPageSize: Int,
      maxPageSize: Int,
  ): EndpointInput[(Int, Option[String])] = query[Int]("page_size")
    .description("Page size")
    .default(defaultPageSize)
    .validate(Validator
      .inRange(1, maxPageSize, minExclusive = false, maxExclusive = false))
    .and(query[Option[String]]("page_token").description("Page token"))
