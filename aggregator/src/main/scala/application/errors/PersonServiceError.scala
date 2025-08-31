package org.aulune.aggregator
package application.errors


import application.PersonService

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur in [[PersonService]].
 *  @param reason string representation of error.
 */
enum PersonServiceError(val reason: String) extends ErrorReason(reason):
  /** Specified person is not found. */
  case PersonNotFound extends PersonServiceError("PERSON_NOT_FOUND")

  /** Given person is not valid person. */
  case InvalidPerson extends PersonServiceError("INVALID_PERSON")
