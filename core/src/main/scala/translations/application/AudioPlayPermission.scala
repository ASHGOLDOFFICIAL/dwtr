package org.aulune
package translations.application


/** Permissions to operation on audio plays. */
enum AudioPlayPermission:
  /** Permission to make writing operations. */
  case Write

  /** Permission to see download links associated with audio plays. Temporary
   *  solution.
   */
  case SeeDownloadLinks
