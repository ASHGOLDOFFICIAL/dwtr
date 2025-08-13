package org.aulune
package translations.api.mappers

import translations.application.dto.ExternalResourceTypeDto


/** Mapper between application layer's [[ExternalResourceTypeDto]] and its API
 *  representation as strings.
 */
private[api] object ExternalResourceTypeMapper:
  private val fromDtoMapper = ExternalResourceTypeDto.values.map {
    case t @ ExternalResourceTypeDto.Purchase  => t -> "purchase"
    case t @ ExternalResourceTypeDto.Streaming => t -> "streaming"
    case t @ ExternalResourceTypeDto.Download  => t -> "download"
    case t @ ExternalResourceTypeDto.Other     => t -> "other"
    case t @ ExternalResourceTypeDto.Private   => t -> "private"
  }.toMap

  private val fromStringMapper = fromDtoMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[ExternalResourceTypeDto]].
   *  @param dto DTO to represent.
   */
  def toString(dto: ExternalResourceTypeDto): String = fromDtoMapper(dto)

  /** Returns [[ExternalResourceTypeDto]] for given string if valid.
   *  @param str string.
   *  @return [[ExternalResourceTypeDto]] or `None` if given string is not
   *    mapped to any DTO object.
   */
  def fromString(str: String): Option[ExternalResourceTypeDto] =
    fromStringMapper.get(str)
