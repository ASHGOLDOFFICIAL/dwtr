package org.aulune
package commons.service.permission


/** Permission type to implement by clients.
 *  @param namespace permission namespace to avoid collisions between services.
 *  @param name name of permission inside namespace.
 *  @param description human readable description.
 */
trait Permission(
    val namespace: String,
    val name: String,
    val description: String,
)
