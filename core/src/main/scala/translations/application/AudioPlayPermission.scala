package org.aulune
package translations.application

import shared.permission.Permission


/** Permissions to operation on audio plays. */
enum AudioPlayPermission(val name: String) extends Permission:
  /** Permission to make writing operations. */
  case Modify extends AudioPlayPermission("modify")

  /** Permission to download audio plays. */
  case SeeDownloadLinks extends AudioPlayPermission("download")
