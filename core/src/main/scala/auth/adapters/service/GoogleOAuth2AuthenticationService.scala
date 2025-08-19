package org.aulune
package auth.adapters.service


import auth.AuthConfig
import auth.adapters.service.GoogleOAuth2AuthenticationService.GoogleOpenIdConfig
import auth.application.OAuth2AuthenticationService
import auth.application.dto.OAuth2Provider.Google

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.http4s.{EntityDecoder, Method, Request, Uri, UrlForm}
import pdi.jwt.{JwtCirce, JwtClaim, JwtOptions}

import scala.util.Try


/** Services managing authorization code exchange with Google. */
object GoogleOAuth2AuthenticationService:
  /** Builds a service.
   *  @param googleClient Google client config.
   *  @param client [[Client]] to make requests.
   *  @tparam F effect type.
   */
  def build[F[_]: Concurrent](
      googleClient: AuthConfig.OAuth2.GoogleClient,
      client: Client[F],
  ): F[OAuth2AuthenticationService[F, Google]] =
    for config <- getGoogleOpenIdConfig(client)
    yield new GoogleOAuth2AuthenticationService[F](config, googleClient, client)

  /** Retrives Google's OpenID configuration.
   *  @param client [[Client]] to make requests with.
   *  @tparam F effect type.
   *  @return Google's Discovery document.
   *  @note It will throw in case of failed request.
   */
  private def getGoogleOpenIdConfig[F[_]: Concurrent](
      client: Client[F],
  ): F[GoogleOpenIdConfig] =
    val request = Request[F](
      method = Method.GET,
      uri = discoveryDocumentUrl,
    )
    client.expect[GoogleOpenIdConfig](request)

  private val discoveryDocumentUrl =
    uri"https://accounts.google.com/.well-known/openid-configuration"

  /** Fields from Google's discovery document that we care about.
   *
   *  @param issuer URL using the `https` scheme with no query or fragment
   *    components that the OP asserts as its Issuer Identifier. This also MUST
   *    be identical to the iss Claim value in ID Tokens issued from this
   *    Issuer.
   *  @param authorizationEndpoint URL of the OP's OAuth 2.0 Authorization
   *    Endpoint. This URL MUST use the `https` scheme and MAY contain port,
   *    path, and query parameter components.
   *  @param tokenEndpoint URL of the OP's OAuth 2.0 Token Endpoint. This is
   *    REQUIRED unless only the Implicit Flow is used. This URL MUST use the
   *    `https` scheme and MAY contain port, path, and query parameter
   *    components.
   *  @param jwksUri URL of the OP's JWK Set document, which MUST use the
   *    `https` scheme. This contains the signing key(s) the RP uses to validate
   *    signatures from the OP.
   *  @param userinfoEndpoint URL of the OP's UserInfo Endpoint. This URL MUST
   *    use the `https` scheme and MAY contain port, path, and query parameter
   *    components. It's RECOMMENDED.
   *  @see
   *    [[https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata OpenID documentation]].
   */
  private final case class GoogleOpenIdConfig(
      issuer: Uri,
      authorizationEndpoint: Uri,
      tokenEndpoint: Uri,
      jwksUri: Uri,
      userinfoEndpoint: Option[Uri],
  )

  private given [F[_]: Concurrent]: EntityDecoder[F, GoogleOpenIdConfig] =
    jsonOf[F, GoogleOpenIdConfig]
  private given Decoder[GoogleOpenIdConfig] = Decoder.instance { cursor =>
    for
      issuer <- cursor.get[Uri]("issuer")
      authorizationEndpoint <- cursor.get[Uri]("authorization_endpoint")
      tokenEndpoint <- cursor.get[Uri]("token_endpoint")
      jwksUri <- cursor.get[Uri]("jwks_uri")
      userinfoEndpoint <- cursor.get[Option[Uri]]("userinfo_endpoint")
    yield GoogleOpenIdConfig(
      issuer = issuer,
      authorizationEndpoint = authorizationEndpoint,
      tokenEndpoint = tokenEndpoint,
      jwksUri = jwksUri,
      userinfoEndpoint = userinfoEndpoint)
  }
  private given Decoder[Uri] = Decoder.decodeString.emap { str =>
    Uri.fromString(str).leftMap(pf => s"Failed to decode Url: $pf")
  }
end GoogleOAuth2AuthenticationService


private final class GoogleOAuth2AuthenticationService[
    F[_]: Concurrent,
] private (
    openIdConfig: GoogleOpenIdConfig,
    googleClient: AuthConfig.OAuth2.GoogleClient,
    client: Client[F],
) extends OAuth2AuthenticationService[F, Google]:

  override def getId(authorizationCode: String): F[Option[String]] =
    exchangeCodeForToken(authorizationCode).map { maybeResponse =>
      for
        response <- maybeResponse
        token <- decodeIdTokenPayload(response.idToken)
      yield token.sub
    }

  /** Returns result of authorization code exchange with Google services.
   *  @param code authorization code.
   */
  private def exchangeCodeForToken(code: String): F[Option[ExchangeResponse]] =
    val form = UrlForm(
      "code" -> code,
      "client_id" -> googleClient.clientId,
      "client_secret" -> googleClient.secret,
      "redirect_uri" -> googleClient.redirectUrl,
      "grant_type" -> "authorization_code",
    )
    val request = Request[F](
      method = Method.POST,
      uri = openIdConfig.tokenEndpoint,
    ).withEntity(form)
    client.expectOption[ExchangeResponse](request)

  /** Returns [[IdTokenPayload]] parsed from given string.
   *  @param token JWT token as string.
   *  @note signature verification is not implemented here and should be added
   *    for production use.
   */
  private def decodeIdTokenPayload(token: String): Option[IdTokenPayload] =
    JwtCirce
      .decode(token, JwtOptions(signature = false)) // TODO: check signature
      .flatMap(tryIdTokenPayload)
      .toOption

  /** Tries to convert [[JwtClaim]] to [[IdTokenPayload]].
   *  @param claim [[JwtClaim]].
   */
  private def tryIdTokenPayload(claim: JwtClaim): Try[IdTokenPayload] = Try(
    IdTokenPayload(
      aud = claim.audience.get.head,
      exp = claim.expiration.get,
      iat = claim.issuedAt.get,
      iss = claim.issuer.get,
      sub = claim.subject.get,
    ))

  /** Google response representation.
   *
   *  @see
   *    [[https://developers.google.com/identity/openid-connect/openid-connect#exchangecode Google documentation]].
   */
  private final case class ExchangeResponse(
      accessToken: String,
      expiresIn: Int,
      scope: String,
      tokenType: String,
      idToken: String,
  )

  /** Google's ID token's payload representation. Only always provided claims
   *  are used.
   *
   *  @see
   *    [[https://developers.google.com/identity/openid-connect/openid-connect#obtainuserinfo Google documentation]].
   */
  private final case class IdTokenPayload(
      aud: String,
      exp: Long,
      iat: Long,
      iss: String,
      sub: String,
  )

  private given EntityDecoder[F, ExchangeResponse] = jsonOf[F, ExchangeResponse]

  private given Decoder[ExchangeResponse] = Decoder.instance { cursor =>
    for
      accessToken <- cursor.get[String]("access_token")
      expiresIn <- cursor.get[Int]("expires_in")
      scope <- cursor.get[String]("scope")
      tokenType <- cursor.get[String]("token_type")
      idToken <- cursor.get[String]("id_token")
    yield ExchangeResponse(
      accessToken = accessToken,
      expiresIn = expiresIn,
      scope = scope,
      tokenType = tokenType,
      idToken = idToken)
  }
