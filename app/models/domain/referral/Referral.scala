package models.domain.referral

import java.util.UUID
import play.api.libs.json._

case class Referral(id: UUID, name: String, imgURL: String, genre: UUID, description: Option[String])

object Referral {
	implicit def implReferral = Json.format[Referral]
}