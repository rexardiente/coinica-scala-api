package models.domain

import play.api.libs.json._

case class InEvent(id: JsValue, input: JsValue)
case class OutEvent(id: JsValue, response: JsValue)

object Event extends utils.CommonImplicits