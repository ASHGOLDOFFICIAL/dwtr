package org.aulune
package auth.api.mappers

import auth.application.dto.OAuth2Provider


/** Mapper between application layer's [[OAuth2Provider]] and its API
 *  representation as strings.
 */
private[api] object OAuth2ProviderMapper:
  private val fromDtoMapper = OAuth2Provider.values.map {
    case t @ OAuth2Provider.Google => t -> "google"
  }.toMap

  private val fromStringMapper = fromDtoMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[OAuth2Provider]].
   *  @param dto DTO to represent.
   */
  def toString(dto: OAuth2Provider): String = fromDtoMapper(dto)

  /** Returns [[OAuth2Provider]] for given string if valid.
   *  @param str string.
   *  @return [[OAuth2Provider]] or `None` if given string is not mapped to any
   *    DTO object.
   */
  def fromString(str: String): Option[OAuth2Provider] =
    fromStringMapper.get(str)
