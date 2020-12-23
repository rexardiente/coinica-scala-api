package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Ranking(id: UUID, name: String,  bets: Double, profit: Double, multiplieramount: Double, rankingcreated: Long)

object Ranking {
	implicit def implRanking = Json.format[Ranking]
}