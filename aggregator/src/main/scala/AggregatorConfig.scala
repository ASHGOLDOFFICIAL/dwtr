package org.aulune.aggregator


/** Config for aggregator app.
 *  @param pagination pagination parameters.
 */
final case class AggregatorConfig(
    pagination: AggregatorConfig.Pagination,
)


object AggregatorConfig:
  final case class Pagination(max: Int, default: Int)
