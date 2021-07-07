package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object ReferralHistory {
	implicit def implReferral = Json.format[ReferralHistory]
}
// case class ReferralHistory(id: UUID, code: String, applied_by: String, fee: Double, status: Boolean, created_at: Instant)
case class ReferralHistory(id: UUID, code: String, applied_by: UUID, created_at: Instant) {
	def toJson(): JsValue = Json.toJson(this)
	def toReferralHistoryJSON(applied_by: String): JsValue =
		Json.obj("id" -> id, "code" -> code, "applied_by" -> applied_by, "created_at" -> created_at)
}