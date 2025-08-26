package org.aulune
package translations.api.mappers

import translations.application.dto.AudioPlayTranslationTypeDto


/** Mapper between application layer's [[AudioPlayTranslationTypeDto]] and its
 *  API representation as strings.
 */
private[api] object AudioPlayTranslationTypeMapper:
  private val fromStringMapper = Map(
    "transcript" -> AudioPlayTranslationTypeDto.Transcript,
    "subtitles" -> AudioPlayTranslationTypeDto.Subtitles,
    "voiceover" -> AudioPlayTranslationTypeDto.VoiceOver,
  )
  private val fromDtoMapper = fromStringMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[AudioPlayTranslationTypeDto]].
   *
   *  @param dto DTO to represent.
   */
  def toString(dto: AudioPlayTranslationTypeDto): String = fromDtoMapper(dto)

  /** Returns [[AudioPlayTranslationTypeDto]] for given string if valid.
   *
   *  @param str string.
   *  @return [[AudioPlayTranslationTypeDto]] or `None` if given string is not
   *    mapped to any DTO object.
   */
  def fromString(str: String): Option[AudioPlayTranslationTypeDto] =
    fromStringMapper.get(str)
