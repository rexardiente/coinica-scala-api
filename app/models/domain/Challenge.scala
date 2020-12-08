package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Challenge(id: UUID, name : String, gameID: UUID,  reward: Double, rankname: String, challengecreated: Long)

object Challenge {
	implicit def implChallenge = Json.format[Challenge]
}