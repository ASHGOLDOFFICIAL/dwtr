package org.aulune.aggregator
package application.dto.person


/** Response to batch get of persons.
 *  @param persons found persons.
 */
final case class BatchGetPersonsResponse(
    persons: List[PersonResource],
)
