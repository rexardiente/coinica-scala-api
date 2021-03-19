package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Challenge extends utils.CommonImplicits {
	val tupled = (apply: (UUID, UUID, String, Instant, Instant) => Challenge).tupled
	def apply(id: UUID,
						gameID: UUID,
						description: String,
						createdAt: Instant,
						expiredAt: Instant): Challenge =
		new Challenge(id, gameID, description, createdAt, expiredAt)
	def apply(gameID: UUID, // title
						description: String,
						createdAt: Instant,
						expiredAt: Instant): Challenge =
		new Challenge(UUID.randomUUID, gameID, description, createdAt, expiredAt)
}
// daily challenge well be randomly selected
// players with highest wagered and bets will be placed on the ranking
// earned points based on ranking..
case class Challenge(id: UUID,
										gameID: UUID, // title
										description: String,
										createdAt: Instant,
										expiredAt: Instant)

object ChallengeTracker extends utils.CommonImplicits {
	val tupled = (apply: (String, UUID, Double, Double, Double, Double) => ChallengeTracker).tupled
}
object ChallengeHistory extends utils.CommonImplicits {
	val tupled = (apply: (UUID, UUID, Seq[ChallengeTracker], Instant) => ChallengeHistory).tupled
	def apply(id: UUID, challengeID: UUID, rank_users: Seq[ChallengeTracker], createdAt: Instant): ChallengeHistory =
			new ChallengeHistory(id, challengeID, rank_users, Instant.now)
	def apply(challengeID: UUID, rank_users: Seq[ChallengeTracker], vipPoints: Double): ChallengeHistory =
		new ChallengeHistory(UUID.randomUUID, challengeID, rank_users, Instant.now)
}

case class ChallengeTracker(user: String, challengeID: UUID, bets: Double, wagered: Double, ratio: Double, prize: Double) {
	def toJson(): JsValue = Json.toJson(this)
}
case class ChallengeHistory(id: UUID, challengeID: UUID, rank_users: Seq[ChallengeTracker], createdAt: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}