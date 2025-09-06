package org.aulune.aggregator
package application.dto.person


/** Response to list request.
 *  @param persons list of persons.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class ListPersonsResponse(
    persons: List[PersonResource],
    nextPageToken: Option[String],
)
