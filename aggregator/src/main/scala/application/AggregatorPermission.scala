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

  /** Permission to download audio plays. */
  case DownloadAudioPlays
      extends AggregatorPermission(
        "download",
        "Allows to download audio plays from inner servers.")
