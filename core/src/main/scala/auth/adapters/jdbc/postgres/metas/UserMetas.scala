package org.aulune
package auth.adapters.jdbc.postgres.metas


import auth.domain.model.{Group, Username}

import doobie.Meta


private[postgres] object UserMetas:
  given Meta[Username] = Meta[String].tiemap { str =>
    Username(str)
      .toRight(s"Failed to decode Username from: $str.")
  }(identity)

  private val groupToInt = Group.values.map {
    case t @ Group.Admin   => t -> 1
    case t @ Group.Trusted => t -> 2
  }.toMap
  private val groupFromInt = groupToInt.map(_.swap)

  given Meta[Group] = Meta[Int]
    .timap(groupFromInt.apply)(groupToInt.apply)

  given Meta[Set[Group]] = Meta[String]
    .timap { str =>
      if str.isEmpty then Set.empty
      else str.split(',').map(s => groupFromInt(s.toInt)).toSet
    }(_.map(groupToInt).mkString(","))
