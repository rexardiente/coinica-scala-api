package models.domain.eosio

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object TreasureHuntGameDataPanelSet extends utils.CommonImplicits
object TreasureHuntGameData extends utils.CommonImplicits

case class TreasureHuntGameDataPanelSet(key: Int, isopen: Int, iswin: Int)
case class TreasureHuntGameData(panel_set: Seq[TreasureHuntGameDataPanelSet],
                                unopentile: Int,
                                win_count: Int,
                                destination: Int,
                                status: Int,
                                enemy_count: Int,
                                prize: BigDecimal,
                                odds: BigDecimal,
                                nextprize: BigDecimal,
                                maxprize : BigDecimal) {
  def toJson(): JsValue = Json.toJson(this)
}