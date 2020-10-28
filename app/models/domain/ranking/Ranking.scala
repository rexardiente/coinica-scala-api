package models.domain.ranking

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

case class Ranking(id: UUID,  rankname: Option[String], rankdesc: String)

object Ranking {
	implicit def implRanking = Json.format[Ranking]
}