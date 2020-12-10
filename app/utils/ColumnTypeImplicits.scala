package utils

import java.time.Instant
import java.sql.Timestamp
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ Act, ActionTrace, Data, Partial, Receipt, Trace}

trait ColumnTypeImplicits extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  protected val dbConfigProvider: DatabaseConfigProvider
  import profile.api._
  implicit val jsValueMappedColumnType = MappedColumnType.base[JsValue, String](
    Json.stringify, Json.parse)
  implicit val traceColumnMapper = MappedColumnType.base[Trace, JsValue](
     s => Json.toJson(s),   
     i => i.as[Trace])
  implicit val stringListMapper = MappedColumnType.base[Seq[ActionTrace], JsValue](
    list => JsArray(list.map(Json.toJson(_))),
    js => js.as[Seq[ActionTrace]])
  implicit val partialColumnMapper = MappedColumnType.base[Partial, JsValue](
     s => Json.toJson(s),   
     i => i.as[Partial])
  implicit val dataColumnMapper = MappedColumnType.base[Data, JsValue](
     s => Json.toJson(s),   
     i => i.as[Data])
  implicit val receiptColumnMapper = MappedColumnType.base[Receipt, JsValue](
     s => Json.toJson(s),   
     i => i.as[Receipt])
  implicit val actColumnMapper = MappedColumnType.base[Act, JsValue](
     s => Json.toJson(s),   
     i => i.as[Act])
  implicit val jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
    dateTime => new Timestamp(dateTime.getMillis),
    timeStamp => new DateTime(timeStamp.getTime)
  )
  implicit val instantColumnType: BaseColumnType[Instant] =
    MappedColumnType.base[Instant, Timestamp](
        instant => Timestamp.from(instant),
        ts => ts.toInstant
    )
}