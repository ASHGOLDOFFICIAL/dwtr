package org.aulune.aggregator


/** Config for aggregator app.
 *  @param maxBatchGet max allowed elements for batch get.
 *  @param pagination pagination parameters.
 *  @param search search parameters.
 *  @param coverS3 config for cover images S3.
 */
final case class AggregatorConfig(
    maxBatchGet: Int,
    pagination: AggregatorConfig.PaginationParams,
    search: AggregatorConfig.SearchParams,
    coverStorage: AggregatorConfig.CoverStorage,
)


object AggregatorConfig:
  final case class PaginationParams(max: Int, default: Int)

  final case class SearchParams(max: Int, default: Int)

  final case class CoverStorage(endpoint: String, bucket: String)
