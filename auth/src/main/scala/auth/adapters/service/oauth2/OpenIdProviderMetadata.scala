package org.aulune
package auth.adapters.service.oauth2

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Method, Request, Uri}


/** Open ID provider metadata we care about.
 *
 *  @param issuer URL using the `https` scheme with no query or fragment
 *    components that the OP asserts as its Issuer Identifier. This also MUST be
 *    identical to the iss Claim value in ID Tokens issued from this Issuer.
 *  @param authorizationEndpoint URL of the OP's OAuth 2.0 Authorization
 *    Endpoint. This URL MUST use the `https` scheme and MAY contain port, path,
 *    and query parameter components.
 *  @param tokenEndpoint URL of the OP's OAuth 2.0 Token Endpoint. This is
 *    REQUIRED unless only the Implicit Flow is used. This URL MUST use the
 *    `https` scheme and MAY contain port, path, and query parameter components.
 *  @param jwksUri URL of the OP's JWK Set document, which MUST use the `https`
 *    scheme. This contains the signing key(s) the RP uses to validate
 *    signatures from the OP.
 *  @param userinfoEndpoint URL of the OP's UserInfo Endpoint. This URL MUST use
 *    the `https` scheme and MAY contain port, path, and query parameter
 *    components. It's RECOMMENDED.
 *  @see
 *    [[https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata OpenID documentation]].
 */
private[oauth2] final case class OpenIdProviderMetadata(
    issuer: Uri,
    authorizationEndpoint: Uri,
    tokenEndpoint: Uri,
    jwksUri: Uri,
    userinfoEndpoint: Option[Uri],
)


private[oauth2] object OpenIdProviderMetadata:
  /** Retrieves OpenID provider metadata from URI.
   *  @param client [[Client]] to make requests with.
   *  @tparam F effect type.
   *  @note It will throw in case of failed request.
   */
  def fetch[F[_]: Concurrent](
      client: Client[F],
      uri: Uri,
  ): F[OpenIdProviderMetadata] =
    val request = Request[F](method = Method.GET, uri = uri)
    client.expect[OpenIdProviderMetadata](request)

  private given [F[_]: Concurrent]: EntityDecoder[F, OpenIdProviderMetadata] =
    jsonOf[F, OpenIdProviderMetadata]

  private given Decoder[OpenIdProviderMetadata] = Decoder.instance { cursor =>
    for
      issuer <- cursor.get[Uri]("issuer")
      authorizationEndpoint <- cursor.get[Uri]("authorization_endpoint")
      tokenEndpoint <- cursor.get[Uri]("token_endpoint")
      jwksUri <- cursor.get[Uri]("jwks_uri")
      userinfoEndpoint <- cursor.get[Option[Uri]]("userinfo_endpoint")
    yield OpenIdProviderMetadata(
      issuer = issuer,
      authorizationEndpoint = authorizationEndpoint,
      tokenEndpoint = tokenEndpoint,
      jwksUri = jwksUri,
      userinfoEndpoint = userinfoEndpoint)
  }
  private given Decoder[Uri] = Decoder.decodeString.emap { str =>
    Uri.fromString(str).leftMap(pf => s"Failed to decode Uri: $pf")
  }
