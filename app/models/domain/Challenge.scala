package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object Challenge {
	val tupled = (apply: (UUID, String, String, Instant, Instant, Boolean, Instant) => Challenge).tupled
	def apply(id: UUID,
						name: String,
						description: String,
						startAt: Instant,
						expireAt: Instant,
						isAvailable: Boolean,
						createdAt: Instant): Challenge =
		new Challenge(id, name, description, startAt, expireAt, isAvailable, createdAt)
	def apply(name: String, description: String, startAt: Instant, expireAt: Instant, isAvailable: Boolean): Challenge =
		new Challenge(UUID.randomUUID, name, description, startAt, expireAt, isAvailable, Instant.now)
	implicit def implChallenge = Json.format[Challenge]
}

case class Challenge(id: UUID,
										name: String, // title
										description: String,
										startAt: Instant,
										expireAt: Instant,
										isAvailable: Boolean,
										createdAt: Instant)

object ChallengeHistory {
	val tupled = (apply: (UUID, UUID, Int, String, Double, Double, Double, Double, Instant) => ChallengeHistory).tupled
	def apply(id: UUID,
						challengeID: UUID,
						rank: Int,
						name: String,
						bet: Double,
						profit: Double,
						ratio: Double,
						vipPoints: Double,
						createdAt: Instant): ChallengeHistory =
			new ChallengeHistory(id, challengeID, rank, name, bet, profit, ratio, vipPoints, createdAt)
	def apply(challengeID: UUID,
						rank: Int,
						name: String,
						bet: Double,
						profit: Double,
						ratio: Double,
						vipPoints: Double): ChallengeHistory =
			new ChallengeHistory(UUID.randomUUID, challengeID, rank, name, bet, profit, ratio, vipPoints, Instant.now)
	implicit def implChallengeHistory = Json.format[ChallengeHistory]
}

case class ChallengeHistory(id: UUID,
														challengeID: UUID,
														rank: Int,
														name: String, // title
														bet: Double,
														profit: Double,
														ratio: Double,
														vipPoints: Double,
														createdAt: Instant)