package org.aulune.commons
package utils.imaging


/** Image formats supported by converter.
 *  @param name string representation of format.
 */
enum ImageFormat(val name: String):
  case BMP extends ImageFormat("bmp")
  case JPEG extends ImageFormat("jpeg")
  case WBMP extends ImageFormat("wbmp")
  case PNG extends ImageFormat("png")
  case GIF extends ImageFormat("gif")
  case TIFF extends ImageFormat("tiff")
