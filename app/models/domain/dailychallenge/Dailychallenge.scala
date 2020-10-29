package models.domain.dailychallenge

import java.time.Instant

import java.util.UUID
import play.api.libs.json._

case class Dailychallenge(id: UUID, gamename: String, rankname: Option[String], rankreward: String, challengedate: Instant)

object Dailychallenge {
	implicit def implDailychallenge = Json.format[Dailychallenge]
}