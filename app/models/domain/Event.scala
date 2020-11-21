package models.domain

import play.api.libs.json._

case class InEvent(id: String, input: JsValue)
case class OutEvent(id: String, response: JsValue)

object Event {
	implicit val inEventFormat  = Json.format[InEvent]
  	implicit val outEventFormat = Json.format[OutEvent]
}