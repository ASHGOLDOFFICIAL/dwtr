package org.aulune
package api.tapir


import domain.model.{MediaResourceId, TranslationId}

import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, DecodeResult}


given Codec[String, MediaResourceId, TextPlain] = Codec.string.mapDecode { s =>
  DecodeResult.Value(MediaResourceId.unsafeApply(s))
}(_.string)

given Codec[String, TranslationId, TextPlain] = Codec.string.mapDecode { s =>
  DecodeResult.Value(TranslationId.unsafeApply(s))
}(_.string)
