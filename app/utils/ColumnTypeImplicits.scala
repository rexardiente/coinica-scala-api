package utils

import java.time.Instant
import java.sql.Timestamp
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ Act, ActionTrace, Data, Partial, Receipt, Trace, GQGameStatus }
import models.domain.TransactionType
import models.domain.enum._
import ejisan.kuro.otp.OTPKey
import ejisan.scalauthx.HashedCredential

trait ColumnTypeImplicits extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  protected val dbConfigProvider: DatabaseConfigProvider
  import profile.api._
  implicit val jsValueMappedColumnType = MappedColumnType.base[JsValue, String](
    Json.stringify, Json.parse)
  implicit val traceColumnMapper = MappedColumnType.base[Trace, JsValue](
     s => Json.toJson(s),
     i => i.as[Trace])
  implicit val stringListMapper = MappedColumnType.base[Seq[ActionTrace], JsValue](
    s => JsArray(s.map(Json.toJson(_))),
    i => i.as[Seq[ActionTrace]])
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
  implicit val transactionTypeColumnMapper = MappedColumnType.base[TransactionType, JsValue](
     s => s.toJson(),
     i => i.as[TransactionType])
  implicit val gqListGameStatusColumnMapper = MappedColumnType.base[List[GQGameStatus], JsValue](
    s => Json.toJson(s),
    i => i.as[List[GQGameStatus]])
  implicit val jodaTimeMapping: BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
    dateTime => new Timestamp(dateTime.getMillis),
    timeStamp => new DateTime(timeStamp.getTime))
  implicit val instantColumnType: BaseColumnType[Instant] = MappedColumnType.base[Instant, Timestamp](
    s => Timestamp.from(s),
    i => i.toInstant)

  // Custom Value Mappers..
  implicit val roleMapper = MappedColumnType.base[Roles.Role, String](
    e => e.toString,
    s => Roles.withName(s))
  implicit val optKeyMapper = MappedColumnType.base[OTPKey, String](
    _.toBase32, OTPKey.fromBase32(_))
  implicit val hashedCredentialMapper = MappedColumnType.base[HashedCredential, String](
    _.toString, HashedCredential.fromString(_))

  // Custom VIP Value Mappers..
  implicit val vipMapper = MappedColumnType.base[VIP.value, String](
    e => e.toString,
    s => VIP.withName(s))
  implicit val vipBenefitAmountMapper = MappedColumnType.base[VIPBenefitAmount.value, Int](
    e => (try {
          e.id
        } catch {
          case _: Throwable => 0
        }),
    s => VIPBenefitAmount.withName(s.toString))
  implicit val vipBenefitPointsMapper = MappedColumnType.base[VIPBenefitPoints.value, Int](
    e => (try {
          e.id
        } catch {
          case _: Throwable => 0
        }),
    s => VIPBenefitPoints.withName(s.toString))
}