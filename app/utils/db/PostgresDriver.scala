package utils.db

import com.github.tminglei.slickpg._

trait PostgresDriver extends ExPostgresProfile 
                      with PgArraySupport
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
      
    // override implicit val simpleUUIDListTypeMapper = new SimpleArrayJdbcType[UUID]("uuid").to(_.toList)
    // override implicit val simpleStrListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    // override implicit val simpleLongListTypeMapper = new SimpleArrayJdbcType[Long]("int8").to(_.toList)
    // override implicit val simpleIntListTypeMapper = new SimpleArrayJdbcType[Int]("int4").to(_.toList)
    // override implicit val simpleShortListTypeMapper = new SimpleArrayJdbcType[Short]("int2").to(_.toList)
    // override implicit val simpleFloatListTypeMapper = new SimpleArrayJdbcType[Float]("float4").to(_.toList)
    // override implicit val simpleDoubleListTypeMapper = new SimpleArrayJdbcType[Double]("float8").to(_.toList)
    // override implicit val simpleBoolListTypeMapper = new SimpleArrayJdbcType[Boolean]("bool").to(_.toList)
    // override implicit val simpleTsListTypeMapper = new SimpleArrayJdbcType[Timestamp]("timestamp").to(_.toList)
    // implicit def JsonColumnType[T: ClassTag](implicit reader: RootJsonReader[T], writer: RootJsonWriter[T]) =
    // MappedColumnType.base[T, JsValue]({ obj => writer.write(obj) }, { json => reader.read(json) })

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
