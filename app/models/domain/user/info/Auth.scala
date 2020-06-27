package models.domain.user.info

import play.api.libs.json._

case class Auth[T >: String](account: T, address: T)

object Auth {
	implicit def implAuth = Json.format[Auth[String]]
}
