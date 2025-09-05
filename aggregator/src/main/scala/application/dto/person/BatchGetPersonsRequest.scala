package org.aulune.aggregator
package application.dto.person

import java.util.UUID


/** Request for batch get.
 *  @param names names of resources.
 */
final case class BatchGetPersonsRequest(
    names: List[UUID],
)
