package models.domain

import play.api.libs.json._

object InEvent
object OutEvent
object Event extends utils.CommonImplicits

case class InEvent(id: JsValue, input: JsValue) extends Event
case class OutEvent(id: JsValue, response: JsValue) extends Event
sealed trait Event {
  def toJson(): JsValue = Json.toJson(this)
}