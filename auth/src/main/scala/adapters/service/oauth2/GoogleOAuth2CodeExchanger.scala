package org.aulune.auth
package adapters.service.oauth2


import adapters.utils.CachingFetcher
import domain.errors.OAuthError
import domain.errors.OAuthError.{InvalidToken, Rejected, Unavailable}
import domain.model.OAuth2Provider.Google
import domain.model.{AuthorizationCode, ExternalId}
import domain.services.OAuth2CodeExchanger

import cats.data.EitherT
import cats.effect.Concurrent
import cats.effect.kernel.Clock
import cats.syntax.all.given
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.http4s.{EntityDecoder, Method, Request, Uri, UrlForm}
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.given
import org.typelevel.log4cats.{Logger, LoggerFactory}


/** Services managing authorization code exchange with Google. */
object GoogleOAuth2CodeExchanger:
  /** Builds a service.
   *  @param googleClient Google client config.
   *  @param client [[Client]] to make requests.
   *  @tparam F effect type.
   */
  def build[F[_]: Concurrent: Clock: LoggerFactory](
      googleClient: AuthConfig.OAuth2.GoogleClient,
      client: Client[F],
  ): F[OAuth2CodeExchanger[F, Google]] =
    for
      config <- OpenIdProviderMetadata.fetch(client, discoveryDocumentUrl)
      fetcher <- CachingFetcher.build(client, config.jwksUri)
      service = new GoogleOAuth2CodeExchanger[F](
        config,
        googleClient,
        client,
        fetcher)
    yield service

  private val discoveryDocumentUrl =
    uri"https://accounts.google.com/.well-known/openid-configuration"


private final class GoogleOAuth2CodeExchanger[
    F[_]: Concurrent: Clock: LoggerFactory,
] private (
    openIdConfig: OpenIdProviderMetadata,
    googleClient: AuthConfig.OAuth2.GoogleClient,
    client: Client[F],
    jwkSetFetcher: CachingFetcher[F],
) extends OAuth2CodeExchanger[F, Google]:

  private given Logger[F] = LoggerFactory[F].getLogger

  override def exchangeForId(
      code: AuthorizationCode,
  ): F[Either[OAuthError, ExternalId]] = (for
    _ <- eitherTLogger.info(s"Exchanging OAuth code: $code")
    response <- EitherT
      .fromOptionF(exchangeCodeForToken(code), Rejected)
    jwkString <- jwkSetFetcher.fetch.attemptT
      .leftSemiflatMap(e =>
        for _ <- error"Couldn't fetch JWKs, error: $e"
        yield Unavailable)
    payload <- EitherT(
      IdTokenPayload
        .verify(
          jwkString,
          googleClient.clientId,
          openIdConfig.issuer.renderString)(response.idToken)
        .map(_.toEither))
      .leftSemiflatMap(e =>
        for _ <- warn"Received invalid token: ${response.idToken}, error: $e"
        yield InvalidToken)
    id <- EitherT.fromOption(ExternalId(payload.sub), InvalidToken)
  yield id).value

  /** Returns result of authorization code exchange with Google services.
   *  @param code authorization code.
   */
  private def exchangeCodeForToken(
      code: String,
  ): F[Option[ExchangeResponse]] =
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

    client.run(request).use { response =>
      response
        .attemptAs[ExchangeResponse]
        .leftSemiflatTap { e =>
          val status = response.status.code
          for
            body <- response.bodyText.compile.string
            _ <- Logger[F].warn(
              s"Received bad response from Google: [$status] $body, error: $e")
          yield ()
        }
        .toOption
        .value
    }

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
