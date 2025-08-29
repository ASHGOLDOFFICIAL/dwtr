package org.aulune
package permissions.adapters.jdbc.postgres

import permissions.domain.{
  PermissionDescription,
  PermissionName,
  PermissionNamespace,
}

import doobie.Meta


/** [[Meta]] instances for permission objects. */
private[postgres] object PermissionMetas:
  given Meta[PermissionNamespace] = Meta[String]
    .imap(str => PermissionNamespace.unsafe(str))(identity)

  given Meta[PermissionName] = Meta[String]
    .imap(str => PermissionName.unsafe(str))(identity)

  given Meta[PermissionDescription] = Meta[String]
    .imap(str => PermissionDescription.unsafe(str))(identity)
