package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Challenge extends utils.CommonImplicits {
	val tupled = (apply: (UUID, UUID, String, Long, Long) => Challenge).tupled
	def apply(id: UUID,
						gameID: UUID,
						description: String,
						created_at: Long,
						expiredAt: Long): Challenge =
		new Challenge(id, gameID, description, created_at, expiredAt)
	def apply(gameID: UUID, // title
						description: String,
						created_at: Long,
						expiredAt: Long): Challenge =
		new Challenge(UUID.randomUUID, gameID, description, created_at, expiredAt)
}
// daily challenge well be randomly selected
// players with highest wagered and bets will be placed on the ranking
// earned points based on ranking..
case class Challenge(id: UUID,
										gameID: UUID, // title
										description: String,
										created_at: Long,
										expiredAt: Long)

object ChallengeTracker extends utils.CommonImplicits {
	val tupled = (apply: (UUID, Double, Double, Double, Double) => ChallengeTracker).tupled
}
object ChallengeHistory extends utils.CommonImplicits {
	val tupled = (apply: (UUID, Seq[ChallengeTracker], Long) => ChallengeHistory).tupled
	def apply(id: UUID, rank_users: Seq[ChallengeTracker], created_at: Long): ChallengeHistory =
			new ChallengeHistory(id, rank_users, created_at)
	def apply(rank_users: Seq[ChallengeTracker]): ChallengeHistory =
		new ChallengeHistory(UUID.randomUUID, rank_users, Instant.now.getEpochSecond)
}

case class ChallengeTracker(user: UUID, bets: Double, wagered: Double, ratio: Double, points: Double) {
	def toJson(): JsValue = Json.toJson(this)
}
case class ChallengeHistory(id: UUID, rank_users: Seq[ChallengeTracker], created_at: Long) {
	def toJson(): JsValue = Json.toJson(this)
}