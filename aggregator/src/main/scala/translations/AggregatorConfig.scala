package org.aulune
package translations


/** Config for aggregator app.
 *  @param pagination pagination parameters.
 */
final case class AggregatorConfig(
    pagination: AggregatorConfig.Pagination,
)


object AggregatorConfig:
  case class Pagination(max: Int, default: Int)
