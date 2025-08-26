package org.aulune
package shared.adapters.doobie


import doobie.Fragment
import doobie.implicits.toSqlInterpolator


/** `DELETE FROM `[[table]].
 *  @param table table name.
 */
inline def deleteF(inline table: String): Fragment =
  fr"DELETE FROM" ++ Fragment.const(table)


/** `INSERT INTO `[[table]]` (`[[col]]`, `[[cols]]`*)`.
 *  @param table table name.
 *  @param col first column name (to ensure at least one is given).
 *  @param cols other column names.
 */
inline def insertF(inline table: String)(
    inline col: String,
    inline cols: String*,
): Fragment = fr"INSERT INTO" ++ Fragment.const(table) ++
  fr0"(" ++ Fragment.const0(cols.foldLeft(col)(_ + ", " + _)) ++ fr")"


/** `SELECT` */
inline def selectF: Fragment = fr"SELECT"


/** `SELECT `[[cols]]`* FROM `[[table]].
 *  @param table table name.
 *  @param cols column names.
 */
inline def selectF(inline table: String)(
    inline cols: String*,
): Fragment = fr"SELECT" ++ Fragment.const(cols.mkString(", ")) ++
  fr"FROM" ++ Fragment.const(table)


/** `UPDATE `[[table]]` SET key = value, ...`.
 *  @param table table name.
 *  @param kv first key-value pair (to ensure at least one is given).
 *  @param kvs other key-value pairs.
 */
inline def updateF(inline table: String)(
    inline kv: (String, Fragment),
    inline kvs: (String, Fragment)*,
): Fragment =
  def setC(kv: (String, Fragment)) = Fragment.const(kv._1) ++ fr"= " ++ kv._2
  val setPart = kvs
    .map(setC)
    .foldLeft(setC(kv))(_ ++ fr", " ++ _)
  fr"UPDATE" ++ Fragment.const(table) ++ fr"SET" ++ setPart


extension (fr: Fragment)
  /** `ASC`. */
  inline def ascF: Fragment = fr"ASC"

  /** `EXISTS (`[[cond]]`)`. */
  inline def existsF(inline cond: Fragment): Fragment =
    fr ++ fr0"EXISTS (" ++ cond ++ fr")"

  /** `FROM `[[table]]. */
  inline def fromF(inline table: String): Fragment =
    fr ++ fr"FROM" ++ Fragment.const(table)

  /** `VALUES (`[[where]]`)`. */
  inline def valuesF(inline values: Fragment): Fragment =
    fr ++ fr0"VALUES (" ++ values ++ fr")"

  /** `WHERE `[[what]]` `[[cond]]. */
  inline def whereF(inline what: String, inline cond: Fragment): Fragment =
    fr ++ fr"WHERE" ++ Fragment.const(what) ++ cond

  /** `AND `[[what]]` `[[cond]]. */
  inline def andF(inline what: String, inline cond: Fragment): Fragment =
    fr ++ fr"AND" ++ Fragment.const(what) ++ cond

  /** `ORDER BY `[[column]]. */
  inline def orderByF(inline column: String): Fragment =
    fr ++ fr"ORDER BY" ++ Fragment.const(column)

  /** `LIMIT `[[limit]]. */
  inline def limitF(inline limit: Int): Fragment = fr ++ fr"LIMIT $limit"

  /** `OFFSET `[[offset]]. */
  inline def offsetF(inline offset: Int): Fragment = fr ++ fr"OFFSET $offset"
