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

object ChallengeWinner extends utils.CommonImplicits
object ChallengeHistory extends utils.CommonImplicits {
	val tupled = (apply: (UUID, UUID, List[ChallengeWinner], Instant) => ChallengeHistory).tupled
	def apply(id: UUID, challengeID: UUID, winners: List[ChallengeWinner], createdAt: Instant): ChallengeHistory =
			new ChallengeHistory(id, challengeID, winners, Instant.now)
	def apply(challengeID: UUID, winners: List[ChallengeWinner], vipPoints: Double): ChallengeHistory =
		new ChallengeHistory(UUID.randomUUID, challengeID, winners, Instant.now)
}

case class ChallengeWinner(rank: Int, user: String, bets: Double, wagered: Double, ratio: Double, prize: Double)
case class ChallengeHistory(id: UUID, challengeID: UUID, winners: List[ChallengeWinner], createdAt: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}