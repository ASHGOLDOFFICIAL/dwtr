package org.aulune.aggregator
package application.dto.person


/** Body of request to list persons.
 *  @param pageSize maximum expected number of elements.
 *  @param pageToken token to retrieve next page.
 */
final case class ListPersonsRequest(
    pageSize: Option[Int],
    pageToken: Option[String],
)
