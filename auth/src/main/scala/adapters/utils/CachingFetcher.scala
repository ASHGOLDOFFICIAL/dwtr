package org.aulune.auth
package adapters.utils


import adapters.utils.CachingFetcher.{Cache, CachedResponse}

import cats.Monad
import cats.effect.{Clock, Concurrent, Ref}
import cats.syntax.all.*
import org.http4s.EntityDecoder.text
import org.http4s.client.Client
import org.http4s.headers.Expires
import org.http4s.{EntityDecoder, Headers, Response, Uri}

import java.time.Instant


/** Fetcher that caches responses until their expiration which is taken from
 *  `Expires` header.
 */
private[adapters] object CachingFetcher:
  /** Builds caching fetcher for given [[Client]] and URI.
   *  @param client [[Client]] to make requests with.
   *  @param uri URI to fetch resource from.
   *  @tparam F effect type.
   */
  def build[F[_]: Concurrent: Clock](
      client: Client[F],
      uri: Uri,
  ): F[CachingFetcher[F]] = Ref
    .of(None: Option[CachedResponse])
    .map(Cache[F].apply)
    .map(cache => new CachingFetcher[F](client, uri, cache))

  /** Cache storage. Holds up to one cached response.
   *  @param responseRef cached response (if any).
   *  @tparam F effect type.
   */
  private final case class Cache[F[_]: Monad: Clock](
      responseRef: Ref[F, Option[CachedResponse]],
  ):
    /** Returns string if it's present and not expired. */
    def get: F[Option[String]] =
      for
        responseOpt <- responseRef.get
        now <- Clock[F].realTimeInstant
        filtered = responseOpt.filter(_.expiresAt.isAfter(now))
      yield filtered.map(_.body)

    /** Puts new response to cache. */
    def put(response: CachedResponse): F[Unit] = responseRef.set(Some(response))

  /** Cached response.
   *  @param body response body.
   *  @param expiresAt expiration date.
   */
  private final case class CachedResponse(body: String, expiresAt: Instant)


/** Fetches given URL with caching.
 *  @param client client to make requests.
 *  @param uri URI to fetch results from.
 *  @param cache cache object to use.
 *  @tparam F effect type.
 */
final class CachingFetcher[F[_]: Concurrent] private (
    client: Client[F],
    uri: Uri,
    cache: Cache[F],
):
  /** Retrieves string from URI.
   *  @note It will throw in case of failed request.
   */
  def fetch: F[String] =
    for
      cached <- cache.get
      result <- cached match
        case None       => client.get(uri)(parseResponse)
        case Some(body) => body.pure[F]
    yield result

  /** Parses response and saves it to cache.
   *  @param response response to parse.
   *  @return received string.
   */
  private def parseResponse(response: Response[F]): F[String] =
    val expiration = parseExpiration(response.headers)
    for
      body <- response.as[String]
      _ <- cache.put(CachedResponse(body, expiration))
    yield body

  /** Parses `Expires` header to get expiration date.
   *  @param headers response headers.
   *  @return expiration date.
   */
  private def parseExpiration(headers: Headers): Instant =
    headers.get[Expires] match
      case Some(exp) => exp.expirationDate.toInstant
      case None      => Instant.MIN
