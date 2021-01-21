package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Referral(id: UUID, referralname: String,  referrallink: String, rate: Double, feeamount: Double, referralcreated: Long)

object Referral {
	implicit def implReferral = Json.format[Referral] 
}