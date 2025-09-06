package org.aulune.commons
package adapters.tapir

import sttp.tapir.{EndpointInput, query}


/** Query parameters for common things: search, pagination. */
object MethodSpecificQueryParams:
  /** Optional `page_size` and `page_token` params. */
  val pagination: EndpointInput[(Option[Int], Option[String])] =
    query[Option[Int]]("page_size")
      .description("Page size.")
      .and(query[Option[String]]("page_token").description("Page token."))

  /** Required `query` param and optional `limit` param. */
  val search: EndpointInput[(String, Option[Int])] = query[String]("query")
    .description("Search query")
    .and(query[Option[Int]]("limit")
      .description("Maximum needed number of search results."))
