package org.aulune.commons
package search

import types.NonEmptyString


/** Parameters to use for search.
 *
 *  @param query query string.
 *  @param limit number of elements.
 */
final case class SearchParams private[search] (
    query: NonEmptyString,
    limit: Int,
)
