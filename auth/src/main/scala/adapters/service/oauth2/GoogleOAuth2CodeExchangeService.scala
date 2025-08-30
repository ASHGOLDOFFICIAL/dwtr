package org.aulune.auth
package adapters.service.oauth2


import adapters.service.inner.OAuth2CodeExchangeService
import application.dto.OAuth2Provider.Google

import cats.effect.Concurrent
import cats.effect.kernel.Clock
import cats.syntax.all.*
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.http4s.{EntityDecoder, Method, Request, Uri, UrlForm}


/** Services managing authorization code exchange with Google. */
object GoogleOAuth2CodeExchangeService:
  /** Builds a service.
   *  @param googleClient Google client config.
   *  @param client [[Client]] to make requests.
   *  @tparam F effect type.
   */
  def build[F[_]: Concurrent: Clock](
      googleClient: AuthConfig.OAuth2.GoogleClient,
      client: Client[F],
  ): F[OAuth2CodeExchangeService[F, Google]] =
    for
      config <- OpenIdProviderMetadata.fetch(client, discoveryDocumentUrl)
      fetcher <- CachingFetcher.build(client, config.jwksUri)
      service = new GoogleOAuth2CodeExchangeService[F](
        config,
        googleClient,
        client,
        fetcher)
    yield service

  private val discoveryDocumentUrl =
    uri"https://accounts.google.com/.well-known/openid-configuration"


private final class GoogleOAuth2CodeExchangeService[
    F[_]: Concurrent: Clock,
] private (
    openIdConfig: OpenIdProviderMetadata,
    googleClient: AuthConfig.OAuth2.GoogleClient,
    client: Client[F],
    jwkSetFetcher: CachingFetcher[F],
) extends OAuth2CodeExchangeService[F, Google]:

  override def getId(authorizationCode: String): F[Option[String]] =
    for
      response <- exchangeCodeForToken(authorizationCode)
      jwkString <- jwkSetFetcher.fetch
      tokenValidated <- IdTokenPayload.verify(
        jwkString,
        googleClient.clientId,
        openIdConfig.issuer.renderString)(response.idToken)
    yield tokenValidated.toOption.map(_.sub)

  /** Returns result of authorization code exchange with Google services.
   *  @param code authorization code.
   */
  private def exchangeCodeForToken(code: String): F[ExchangeResponse] =
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
    client.expect[ExchangeResponse](request)

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
