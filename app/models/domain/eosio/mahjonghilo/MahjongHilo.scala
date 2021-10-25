package models.domain.eosio

import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object MahjongHiloGameData extends utils.CommonImplicits
case class MahjongHiloScore(score_name: Int, value: Int)
case class MahjongHiloTile(suit: Int, value: Int)
case class MahjongHiloWinnables(index: Int, tileswin: Seq[MahjongHiloTile])
case class MahjongHiloGameData(game_id: String,
                              deck_player: Seq[Int],
                              status: Int,
                              hi_lo_balance: BigDecimal,
                              prediction: Int,
                              hi_lo_result: Int,
                              hi_lo_outcome: Int,
                              hi_lo_bet: BigDecimal,
                              hi_lo_stake: BigDecimal,
                              low_odds: BigDecimal,
                              draw_odds: BigDecimal,
                              high_odds: BigDecimal,
                              bet_status: Int,
                              option_status: Int,
                              riichi_status: Int,
                              sumofvalue: Seq[Int],
                              prevalent_wind: Int,
                              seat_wind: Int,
                              current_tile: Int,
                              standard_tile: Int,
                              eye_idx: Int,
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
                              score_type: Seq[MahjongHiloScore],
                              wintiles: Seq[MahjongHiloWinnables],
                              final_score: Int) {
   def toJson(): JsValue = Json.toJson(this)
}
// table that will keep on track user gamedata
// predictions: Seq[(Int, Int, Int, Int)] -> (prediction, result, predictionTiles)
object MahjongHiloHistory extends utils.CommonImplicits {
  val tupled = (apply: (String,
                        Int,
                        Seq[(Int, Int, Int, Int)],
                        Option[MahjongHiloGameData],
                        Boolean,
                        Instant) => MahjongHiloHistory).tupled
  def apply(gameID: String, userGameID: Int): MahjongHiloHistory =
    new MahjongHiloHistory(gameID, userGameID, Seq.empty, None, false, Instant.now)
  def apply(gameID: String,
            userGameID: Int,
            predictions: Seq[(Int, Int, Int, Int)],
            gameData: Option[MahjongHiloGameData],
            status: Boolean,
            createdAt: Instant): MahjongHiloHistory =
    new MahjongHiloHistory(gameID, userGameID, predictions, gameData, status, createdAt)
}
case class MahjongHiloHistory(gameID: String,
                              userGameID: Int,
                              predictions: Seq[(Int, Int, Int, Int)],
                              gameData: Option[MahjongHiloGameData],
                              status: Boolean,
                              createdAt: Instant) {
  def toJson(): JsValue = Json.toJson(this)
  def toTrimmedJson(): JsValue = Json.obj("game_id" -> gameID,
                                          "user" -> userGameID,
                                          "predictions" -> predictions,
                                          "status" -> status,
                                          "created_at" -> createdAt)
}