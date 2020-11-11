package utils

import javax.inject.{ Inject, Singleton }
import java.time.Instant
import java.sql.Timestamp
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain._

trait Implicits extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  protected val dbConfigProvider: DatabaseConfigProvider
  import profile.api._

  implicit val jsValueMappedColumnType = MappedColumnType.base[JsValue, String](
    Json.stringify, Json.parse)
  implicit val traceColumnMapper = MappedColumnType.base[Trace, JsValue](
     s => s.toJson(),   
     i => i.as[Trace])
  implicit val stringListMapper = MappedColumnType.base[Seq[ActionTrace], JsValue](
    list => JsArray(list.map(_.toJson())),
    js => js.as[Seq[ActionTrace]])
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