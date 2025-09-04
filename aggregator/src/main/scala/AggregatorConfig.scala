package org.aulune.aggregator


/** Config for aggregator app.
 *  @param pagination pagination parameters.
 *  @param search search parameters.
 */
final case class AggregatorConfig(
    pagination: AggregatorConfig.PaginationParams,
    search: AggregatorConfig.SearchParams,
)


object AggregatorConfig:
  final case class PaginationParams(max: Int, default: Int)

  final case class SearchParams(max: Int, default: Int)
