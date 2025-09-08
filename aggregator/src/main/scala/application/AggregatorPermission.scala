package org.aulune.aggregator
package application

import org.aulune.commons.service.permission.Permission


/** Permissions of aggregator module. */
enum AggregatorPermission(name: String, description: String)
    extends Permission(
      namespace = "aggregator",
      name = name,
      description = description):

  /** Permission to make writing operations. */
  case Modify
      extends AggregatorPermission(
        "modify",
        "Allows to modify content and persons inside aggregator module.")

  /** Permission to see links to self-hosted material. */
  case SeeSelfHostedLocation
      extends AggregatorPermission(
        "see_self_hosted",
        "Allows to see self-hosted material.")
