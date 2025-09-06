package org.aulune.aggregator
package api.mappers

import org.aulune.aggregator.application.dto.shared.ExternalResourceTypeDTO


/** Mapper between application layer's [[ExternalResourceTypeDTO]] and its API
 *  representation as strings.
 */
private[api] object ExternalResourceTypeMapper:
  private val fromDtoMapper = ExternalResourceTypeDTO.values.map {
    case t @ ExternalResourceTypeDTO.Purchase  => t -> "purchase"
    case t @ ExternalResourceTypeDTO.Streaming => t -> "streaming"
    case t @ ExternalResourceTypeDTO.Download  => t -> "download"
    case t @ ExternalResourceTypeDTO.Other     => t -> "other"
    case t @ ExternalResourceTypeDTO.Private   => t -> "private"
  }.toMap

  private val fromStringMapper = fromDtoMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[ExternalResourceTypeDTO]].
   *
   *  @param dto DTO to represent.
   */
  def toString(dto: ExternalResourceTypeDTO): String = fromDtoMapper(dto)

  /** Returns [[ExternalResourceTypeDTO]] for given string if valid.
   *
   *  @param str string.
   *  @return [[ExternalResourceTypeDTO]] or `None` if given string is not
   *    mapped to any DTO object.
   */
  def fromString(str: String): Option[ExternalResourceTypeDTO] =
    fromStringMapper.get(str)
