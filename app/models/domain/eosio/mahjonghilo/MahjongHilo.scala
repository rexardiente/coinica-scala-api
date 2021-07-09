package models.domain.eosio

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object MahjongHiloGameData extends utils.CommonImplicits
case class MahjongHiloGameData(game_id: String,
                              deck_player: Seq[Int],
                              status: Int,
                              hi_lo_balance: BigDecimal,
                              hi_lo_result: Int, // Int -> 2 or 3
                              hi_lo_outcome: Int, // Int -> 2 or 3
                              hi_lo_bet: BigDecimal,
                              hi_lo_stake: BigDecimal,
                              low_odds: BigDecimal,
                              draw_odds: BigDecimal,
                              high_odds: BigDecimal,
                              bet_status: Int, // TODO: Int -> true or false
                              option_status: Int, // TODO: Int -> true or false
                              prediction: Int, // Int -> low, draw and high
                              sumofvalue: Seq[Int],
                              prevalent_wind: Int,
                              seat_wind: Int,
                              current_tile: Int,
                              standard_tile: Int,
                              eye_idx: Int,
                              winnable: Int, // TODO: Int -> true or false
                              pair_count: Int,
                              pung_count: Int,
                              chow_count: Int,
                              kong_count: Int,
                              draw_count: Int,
                              hand_player: Seq[Int],
                              discarded_tiles: Seq[Int],
                              reveal_kong: Seq[Int],
                              winning_hand: Seq[Int],
                              score_check: Seq[Int],
                              score_type: Seq[Int],
                              final_score: Int) {
   def toJson(): JsValue = Json.toJson(this)
}