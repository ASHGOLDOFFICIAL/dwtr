package org.aulune.aggregator
package api.mappers

import application.dto.audioplay.translation.AudioPlayTranslationTypeDTO


/** Mapper between application layer's [[AudioPlayTranslationTypeDTO]] and its
 *  API representation as strings.
 */
private[api] object AudioPlayTranslationTypeMapper:
  private val fromStringMapper = Map(
    "transcript" -> AudioPlayTranslationTypeDTO.Transcript,
    "subtitles" -> AudioPlayTranslationTypeDTO.Subtitles,
    "voiceover" -> AudioPlayTranslationTypeDTO.VoiceOver,
  )
  private val fromDtoMapper = fromStringMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[AudioPlayTranslationTypeDTO]].
   *
   *  @param dto DTO to represent.
   */
  def toString(dto: AudioPlayTranslationTypeDTO): String = fromDtoMapper(dto)

  /** Returns [[AudioPlayTranslationTypeDTO]] for given string if valid.
   *
   *  @param str string.
   *  @return [[AudioPlayTranslationTypeDTO]] or `None` if given string is not
   *    mapped to any DTO object.
   */
  def fromString(str: String): Option[AudioPlayTranslationTypeDTO] =
    fromStringMapper.get(str)
