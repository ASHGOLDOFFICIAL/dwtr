package org.aulune.aggregator
package application.dto.person


/** Request to search persons.
 *  @param query query string.
 *  @param limit maximum number of elements required.
 */
final case class SearchPersonsRequest(
    query: String,
    limit: Option[Int],
)
