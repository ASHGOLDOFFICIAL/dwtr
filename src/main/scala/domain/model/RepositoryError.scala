package org.aulune
package domain.model

enum RepositoryError:
  case AlreadyExists
  case NotFound
  case StorageFailure(reason: String)
