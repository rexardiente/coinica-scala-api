package models.domain.eosio

import java.util.UUID
import play.api.libs.json._
// Ranking table
// - character_id
// - current_streak
//   - game_id
//   - time_executed
// - highest_win_streak // if current will be higher than max then max = current_streak
// - updated_at
// - created_at
// object GQCharactersWinStreakList extends utils.CommonImplicits
// case class GQCharactersWinStreakList(game_id: List[String]) extends AnyVal
object GQCharactersLifeTimeWinStreak extends utils.CommonImplicits
object GQCharactersRankByEarned extends utils.CommonImplicits
object GQCharactersRankByWinStreak extends utils.CommonImplicits
case class GQCharactersLifeTimeWinStreak(character_id: String,
																				current_win_streak: List[String], // list of game_id
																				highest_win_streak: List[String],
																				updated_at: Long,
																				created_at: Long) {
	def toJson(): JsValue = Json.toJson(this)
}

case class GQCharactersRankByEarned(id: String,
																		owner: UUID,
																		ghost_class: Int,
																		ghost_level: Int,
																		earned: Double) {
	def toJson(): JsValue = Json.toJson(this)
}
case class GQCharactersRankByWinStreak(id: String,
																			owner: UUID,
																			ghost_class: Int,
																			ghost_level: Int,
																			win_streak: Int) {
	def toJson(): JsValue = Json.toJson(this)
}