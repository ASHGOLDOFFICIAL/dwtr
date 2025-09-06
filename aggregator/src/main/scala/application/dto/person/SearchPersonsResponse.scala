package org.aulune.aggregator
package application.dto.person


/** Response to search request.
 *  @param persons matched persons.
 */
final case class SearchPersonsResponse(
    persons: List[PersonResource],
)
