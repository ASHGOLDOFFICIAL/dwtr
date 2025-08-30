package org.aulune.aggregator
package domain.errors


import domain.model.person.Person

import scala.util.control.NoStackTrace


/** Errors that can occur during [[Person]] validation. */
enum PersonValidationError extends NoStackTrace:
  /** Some given arguments are invalid */
  case InvalidArguments
