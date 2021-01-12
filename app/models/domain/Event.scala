package models.domain

import play.api.libs.json._
import play.api.libs.functional.syntax._

object InEvent extends utils.CommonImplicits
object OutEvent extends utils.CommonImplicits

object Event extends utils.CommonImplicits {
	import InEvent._
  import OutEvent._

  implicit val tempEventR: Reads[Event] = {
    Json.format[InEvent].map(x => x: Event) or
    Json.format[OutEvent].map(x => x: Event)
  }

  implicit val tempEventW = new Writes[Event] {
    def writes(event: Event): JsValue = {
      event match {
        case m: InEvent => Json.toJson(m)
        case m: OutEvent => Json.toJson(m)
        case _ => Json.obj("error" -> "wrong Json")
      }
    }
  }
}

case class InEvent(id: JsValue, input: JsValue) extends Event
case class OutEvent(id: JsValue, response: JsValue) extends Event
sealed trait Event {
  def toJson = Json.toJson(this)
}