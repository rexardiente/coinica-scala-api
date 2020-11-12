package utils

import javax.inject.{ Inject, Singleton }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain._

trait Implicits extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  protected val dbConfigProvider: DatabaseConfigProvider
  import profile.api._

  implicit val jsValueMappedColumnType = MappedColumnType.base[JsValue, String](
    Json.stringify, Json.parse)
  implicit val stringListMapper = MappedColumnType.base[Seq[Trace], JsValue](
    list => JsArray(list.map(_.toJson())),
    js => js.as[Seq[Trace]])
  implicit val partialColumnMapper = MappedColumnType.base[Partial, JsValue](
     s => s.toJson(),   
     i => i.as[Partial])
  implicit val dataColumnMapper = MappedColumnType.base[Data, JsValue](
     s => s.toJson(),   
     i => i.as[Data])
  implicit val receiptColumnMapper = MappedColumnType.base[Receipt, JsValue](
     s => s.toJson(),   
     i => i.as[Receipt])
  implicit val actColumnMapper = MappedColumnType.base[Act, JsValue](
     s => s.toJson(),   
     i => i.as[Act])
}