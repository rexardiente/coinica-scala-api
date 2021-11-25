package utils

import java.time.Instant
import java.sql.Timestamp
import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{
    Act,
    ActionTrace,
    Data,
    Partial,
    Receipt,
    Trace,
    GhostQuestCharacterGameLog,
    GhostQuestCharacterValue,
    MahjongHiloGameData }
import models.domain.{
    ChallengeTracker,
    TaskGameInfo,
    TransactionType,
    RankType,
    PlatformGame,
    PlatformHost,
    PlatformCurrency }
import models.domain.wallet.support.{ Coin, CryptoJsonRpcHistory }
import models.domain.enum._
// import ejisan.kuro.otp.OTPKey
// import ejisan.scalauthx.HashedCredential

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
  implicit val mahjongHiloGameDataColumnMapper = MappedColumnType.base[MahjongHiloGameData, JsValue](
     s => s.toJson(),
     i => i.as[MahjongHiloGameData])
  implicit val ghostQuestCharacterValueColumnMapper = MappedColumnType.base[GhostQuestCharacterValue, JsValue](
     s => s.toJson(),
     i => i.as[GhostQuestCharacterValue])
  implicit val walletCoinColumnMapper = MappedColumnType.base[Coin, JsValue](
     s => s.toJson(),
     i => i.as[Coin])
  implicit val CryptoJsonRpcHistoryColumnMapper = MappedColumnType.base[CryptoJsonRpcHistory, JsValue](
     s => s.toJson(),
     i => i.as[CryptoJsonRpcHistory])
  implicit val listTransactionTypeColumnMapper = MappedColumnType.base[List[TransactionType], JsValue](
    s => Json.toJson(s),
    i => i.as[List[TransactionType]])
  implicit val listPlatformGameColumnMapper = MappedColumnType.base[List[PlatformGame], JsValue](
    s => Json.toJson(s),
    i => i.as[List[PlatformGame]])
  implicit val listPlatformHostColumnMapper = MappedColumnType.base[List[PlatformHost], JsValue](
    s => Json.toJson(s),
    i => i.as[List[PlatformHost]])
  implicit val listPlatformCurrencyColumnMapper = MappedColumnType.base[List[PlatformCurrency], JsValue](
    s => Json.toJson(s),
    i => i.as[List[PlatformCurrency]])
  implicit val gqCharacterGameLogColumnMapper = MappedColumnType.base[List[GhostQuestCharacterGameLog], JsValue](
    s => Json.toJson(s),
    i => i.as[List[GhostQuestCharacterGameLog]])
  implicit val gqBattleResultColumnMapper = MappedColumnType.base[List[(String, (Int, Boolean))], JsValue](
    s => Json.toJson(s),
    i => i.as[List[(String, (Int, Boolean))]])
  implicit val challengeTrackerColumnMapper = MappedColumnType.base[List[ChallengeTracker], JsValue](
    s => Json.toJson(s),
    i => i.as[List[ChallengeTracker]])
  implicit val challengeTrackerMapper = MappedColumnType.base[Seq[ChallengeTracker], JsValue](
    s => JsArray(s.map(Json.toJson(_))),
    i => i.as[Seq[ChallengeTracker]])
  implicit val taskGameInfoMapper = MappedColumnType.base[Seq[TaskGameInfo], JsValue](
    s => JsArray(s.map(Json.toJson(_))),
    i => i.as[Seq[TaskGameInfo]])
  implicit val seqUUIDMapper = MappedColumnType.base[Seq[UUID], JsValue](
    s => JsArray(s.map(Json.toJson(_))),
    i => i.as[Seq[UUID]])
  implicit val seqIntMapper = MappedColumnType.base[Seq[(Int, Int, Int, Int)], JsValue](
    s => JsArray(s.map(Json.toJson(_))),
    i => i.as[Seq[(Int, Int, Int, Int)]])
  implicit val seqRankTypeMapper = MappedColumnType.base[Seq[RankType], JsValue](
    s => JsArray(s.map(Json.toJson(_))),
    i => i.as[Seq[RankType]])
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
  // implicit val optKeyMapper = MappedColumnType.base[OTPKey, String](
  //   _.toBase32, OTPKey.fromBase32(_))
  // implicit val hashedCredentialMapper = MappedColumnType.base[HashedCredential, String](
  //   _.toString, HashedCredential.fromString(_))

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
    s => VIPBenefitAmount.apply(s))
  implicit val vipBenefitPointsMapper = MappedColumnType.base[VIPBenefitPoints.value, Int](
    e => (try {
          e.id
        } catch {
          case _: Throwable => 0
        }),
    s => VIPBenefitPoints.apply(s))
}