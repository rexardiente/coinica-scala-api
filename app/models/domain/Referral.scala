package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Referral(id: UUID, name : String, gameID: UUID,  imgURl: String, amount: Double, referralreated: Long)

object c {
	implicit def implReferral = Json.format[Referral]
}