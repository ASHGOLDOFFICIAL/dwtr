package org.aulune
package translations.application

import shared.service.permission.Permission


/** Permissions of translation module. */
enum TranslationPermission(
    override val name: String,
    override val description: String,
) extends Permission(
      namespace = "translations",
      name = name,
      description = description):

  /** Permission to make writing operations. */
  case Modify
      extends TranslationPermission(
        "modify",
        "Allows to modify content and persons inside translations module.")

  /** Permission to download audio plays. */
  case DownloadAudioPlays
      extends TranslationPermission(
        "download",
        "Allows to download audio plays from inner servers.")
