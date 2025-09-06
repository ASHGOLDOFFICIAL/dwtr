package org.aulune.auth
package api.mappers

import application.dto.OAuth2ProviderDTO


/** Mapper between application layer's [[OAuth2ProviderDTO]] and its API
 *  representation as strings.
 */
private[api] object OAuth2ProviderMapper:
  private val fromDtoMapper = OAuth2ProviderDTO.values.map {
    case t @ OAuth2ProviderDTO.Google => t -> "google"
  }.toMap

  private val fromStringMapper = fromDtoMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[OAuth2Provider]].
   *  @param dto DTO to represent.
   */
  def toString(dto: OAuth2ProviderDTO): String = fromDtoMapper(dto)

  /** Returns [[OAuth2Provider]] for given string if valid.
   *  @param str string.
   *  @return [[OAuth2Provider]] or `None` if given string is not mapped to any
   *    DTO object.
   */
  def fromString(str: String): Option[OAuth2ProviderDTO] =
    fromStringMapper.get(str)
