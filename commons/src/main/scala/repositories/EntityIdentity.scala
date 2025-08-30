package org.aulune.commons
package repositories


/** Type [[E]] has identity of type [[Id]]
 *  @tparam E element type.
 *  @tparam Id identity type.
 */
trait EntityIdentity[E, Id]:
  /** Returns identity of given element.
   *  @param elem element.
   *  @return element's identity.
   */
  def identity(elem: E): Id
