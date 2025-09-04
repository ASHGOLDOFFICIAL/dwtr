package org.aulune.commons
package errors

/** The reason of the error. This is a constant value that identifies the
 *  proximate cause of the error. Error reasons are unique within a particular
 *  domain of errors. This should be at most 63 characters and match a regular
 *  expression of `[A-Z][A-Z0-9_]+[A-Z0-9]`, which represents UPPER_SNAKE_CASE.
 */
trait ErrorReason(val name: String)
