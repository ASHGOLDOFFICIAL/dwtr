package org.aulune.commons
package testing


import errors.ErrorResponse
import errors.ErrorStatus.Internal

import cats.Functor
import cats.syntax.all.given
import org.scalatest.Assertion
import org.scalatest.Assertions.fail
import org.scalatest.matchers.should.Matchers.shouldBe


/** Functions for easy testing of application services' error responses. */
object ErrorAssertions:
  /** Asserts that error response of given reason was returned.
   *  @param result operation whose result is asserted.
   *  @param expectedReason expected error reason.
   *  @tparam F effect type.
   */
  def assertDomainError[F[_]: Functor](result: F[Either[ErrorResponse, ?]])(
      expectedReason: Any,
  ): F[Assertion] = result.map {
    case Left(err) => err.details.info match
        case Some(info) => info.reason shouldBe expectedReason
        case None => fail("Error info should have been attached to response.")
    case Right(_) => fail("Expected error response.")
  }

  /** Asserts that error response with [[Internal]] status was returned.
   *  @param result operation whose result is asserted.
   *  @tparam F effect type.
   */
  def assertInternalError[F[_]: Functor](
      result: F[Either[ErrorResponse, ?]],
  ): F[Assertion] = result.map {
    case Left(err) => err.status shouldBe Internal
    case Right(_)  => fail("Expected internal error.")
  }
