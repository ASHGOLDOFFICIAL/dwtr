package org.aulune
package commons.http

import sttp.tapir.{EndpointInput, Validator, query}


object QueryParams:
  /** Pagination query parameters for Tapir.
   *  @param defaultPageSize default page size.
   *  @param maxPageSize maximum allowed page size.
   */
  def pagination(
      defaultPageSize: Int,
      maxPageSize: Int,
  ): EndpointInput[(Int, Option[String])] = query[Int]("page_size")
    .description("Page size")
    .default(defaultPageSize)
    .validate(Validator
      .inRange(1, maxPageSize, minExclusive = false, maxExclusive = false))
    .and(query[Option[String]]("page_token").description("Page token"))
