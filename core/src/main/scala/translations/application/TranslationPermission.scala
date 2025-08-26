package org.aulune
package translations.application

import shared.service.permission.Permission


/** Permissions of translation module. */
enum TranslationPermission(val name: String) extends Permission:
  /** Permission to make writing operations. */
  case Modify extends TranslationPermission("modify")

  /** Permission to download audio plays. */
  case DownloadAudioPlays extends TranslationPermission("download")
