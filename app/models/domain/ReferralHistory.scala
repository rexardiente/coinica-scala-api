package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

// case class ReferralHistory(id: UUID, code: String, applied_by: String, fee: Double, status: Boolean, created_at: Instant)
case class ReferralHistory(id: UUID, code: String, applied_by: UUID, created_at: Instant)

object ReferralHistory {
	implicit def implReferral = Json.format[ReferralHistory]
}