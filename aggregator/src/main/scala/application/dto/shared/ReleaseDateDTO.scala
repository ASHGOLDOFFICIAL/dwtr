package org.aulune.aggregator
package application.dto.shared


import application.dto.shared.ReleaseDateDTO.DateAccuracyDTO

import java.time.LocalDate


/** Release date of something.
 *  @param date date itself.
 *  @param accuracy degree of accuracy.
 */
final case class ReleaseDateDTO(date: LocalDate, accuracy: DateAccuracyDTO)


object ReleaseDateDTO:
  /** Tells how accurate given date is. */
  enum DateAccuracyDTO:
    /** Date is fully accurate. */
    case Full

    /** Year and all below can be inaccurate. */
    case Year

    /** Month and all below can be inaccurate. */
    case Month

    /** Day and all below can be inaccurate. */
    case Day
