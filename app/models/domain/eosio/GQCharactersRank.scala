package models.domain.eosio

import play.api.libs.json._
// Ranking table
// - character_id
// - current_streak
//   - game_id
//   - time_executed
// - highest_win_streak // if current will be higher than max then max = current_streak
// - updated_at
// - created_at
// object GhostQuestCharactersWinStreakList extends utils.CommonImplicits
// case class GhostQuestCharactersWinStreakList(game_id: List[String]) extends AnyVal
object GhostQuestCharactersLifeTimeWinStreak extends utils.CommonImplicits
object GhostQuestCharactersRankByEarned extends utils.CommonImplicits
object GhostQuestCharactersRankByWinStreak extends utils.CommonImplicits
case class GhostQuestCharactersLifeTimeWinStreak(character_id: String,
																				current_win_streak: List[String], // list of game_id
																				highest_win_streak: List[String],
																				updated_at: Long,
																				created_at: Long) {
	def toJson(): JsValue = Json.toJson(this)
}

case class GhostQuestCharactersRankByEarned(key: String,
																						ghost_id: Int,
																						owner: Int,
																						rarity: Int,
																						earned: Double) {
	def toJson(): JsValue = Json.toJson(this)
}
case class GhostQuestCharactersRankByWinStreak(key: String,
																							ghost_id: Int,
																							owner: Int,
																							rarity: Int,
																							win_streak: Int) {
	def toJson(): JsValue = Json.toJson(this)
}