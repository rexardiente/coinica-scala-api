package utils.db

import com.github.tminglei.slickpg._

trait PostgresDriver extends ExPostgresProfile with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {
  
  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with DateTimeImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants {
    implicit val intListTypeMapper = new SimpleArrayJdbcType[Int]("integer").to(_.toList)
    implicit val intListListTypeMapper = new AdvancedArrayJdbcType[List[Int]]("integer[]",
      utils.SimpleArrayUtils.fromString[List[Int]](s => 
        scala.util.Try(s.substring(5, s.length - 1).split(",").map(_.trim.toInt).toList).getOrElse(List())
      )(_).orNull,
      utils.SimpleArrayUtils.mkString[List[Int]](_.toString)
    ).to(_.toList)
  }
}

object PostgresDriver extends PostgresDriver
