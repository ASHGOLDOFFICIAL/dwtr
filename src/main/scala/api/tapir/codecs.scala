package org.aulune
package api.tapir


import domain.model.MediaResourceId

import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, DecodeResult}


given Codec[String, MediaResourceId, TextPlain] = Codec.string.mapDecode { s =>
  DecodeResult.Value(MediaResourceId.unsafeApply(s))
}(_.string)
