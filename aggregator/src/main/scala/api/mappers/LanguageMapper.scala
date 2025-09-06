package org.aulune.aggregator
package api.mappers

import org.aulune.aggregator.application.dto.shared.LanguageDTO


/** Mapper between application layer's [[LanguageDTO]] and its API
 *  representation as strings.
 */
private[api] object LanguageMapper:
  private val fromStringMapper = Map(
    "rus" -> LanguageDTO.Russian,
    "ukr" -> LanguageDTO.Ukrainian,
  )
  private val fromDtoMapper = fromStringMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[LanguageDTO]].
   *
   *  @param dto DTO to represent.
   */
  def toString(dto: LanguageDTO): String = fromDtoMapper(dto)

  /** Returns [[LanguageDTO]] for given string if valid.
   *
   *  @param str string.
   *  @return [[LanguageDTO]] or `None` if given string is not mapped to any DTO
   *    object.
   */
  def fromString(str: String): Option[LanguageDTO] = fromStringMapper.get(str)
