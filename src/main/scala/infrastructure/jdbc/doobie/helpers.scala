package org.aulune
package infrastructure.jdbc.doobie


import doobie.Fragment
import doobie.implicits.toSqlInterpolator


inline def deleteF(inline from: String): Fragment =
  fr"DELETE FROM" ++ Fragment.const(from)


inline def insertF(inline into: String)(
    inline col: String,
    inline cols: String*
): Fragment = fr"INSERT INTO" ++ Fragment.const(into) ++
  fr0"(" ++ Fragment.const0(cols.foldLeft(col)(_ + ", " + _)) ++ fr")"


inline def selectF: Fragment = fr"SELECT"


inline def selectF(inline from: String)(
    inline cols: String*
): Fragment = fr"SELECT" ++ Fragment.const(cols.mkString(", ")) ++
  fr"FROM" ++ Fragment.const(from)


inline def updateF(inline table: String)(
    inline kv: (String, Fragment),
    inline kvs: (String, Fragment)*
): Fragment =
  def setC(kv: (String, Fragment)) = Fragment.const(kv._1) ++ fr"= " ++ kv._2
  val setPart                      = kvs
    .map(setC)
    .foldLeft(setC(kv))(_ ++ fr", " ++ _)
  fr"UPDATE" ++ Fragment.const(table) ++ fr"SET" ++ setPart


extension (fr: Fragment)
  inline def ascF: Fragment = fr"ASC"

  inline def existsF(inline cond: Fragment): Fragment =
    fr ++ fr0"(" ++ cond ++ fr")"

  inline def fromF(inline where: String): Fragment =
    fr ++ fr"FROM" ++ Fragment.const(where)

  inline def valuesF(inline values: Fragment): Fragment =
    fr ++ fr0"VALUES (" ++ values ++ fr")"

  inline def whereF(inline what: String, inline cond: Fragment): Fragment =
    fr ++ fr"WHERE" ++ Fragment.const(what) ++ cond

  inline def andF(inline what: String, inline cond: Fragment): Fragment =
    fr ++ fr"AND" ++ Fragment.const(what) ++ cond

  inline def orderByF(inline by: String): Fragment =
    fr ++ fr"ORDER BY" ++ Fragment.const(by)

  inline def limitF(inline limit: Int): Fragment = fr ++ fr"LIMIT $limit"

  inline def offsetF(inline offset: Int): Fragment = fr ++ fr"OFFSET $offset"
