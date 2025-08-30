package org.aulune.aggregator
package api.mappers

import application.dto.LanguageDto


/** Mapper between application layer's [[LanguageDto]] and its API
 *  representation as strings.
 */
private[api] object LanguageMapper:
  private val fromStringMapper = Map(
    "rus" -> LanguageDto.Russian,
    "ukr" -> LanguageDto.Ukrainian,
  )
  private val fromDtoMapper = fromStringMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[LanguageDto]].
   *
   *  @param dto DTO to represent.
   */
  def toString(dto: LanguageDto): String = fromDtoMapper(dto)

  /** Returns [[LanguageDto]] for given string if valid.
   *
   *  @param str string.
   *  @return [[LanguageDto]] or `None` if given string is not mapped to any DTO
   *    object.
   */
  def fromString(str: String): Option[LanguageDto] = fromStringMapper.get(str)
