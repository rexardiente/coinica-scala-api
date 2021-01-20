package models.domain

import play.api.libs.json._


object InEventMessage extends utils.CommonImplicits
object GQBattleTime
object GQCharacterCreated

sealed trait InEventMessage {
  def toJson(): JsValue = Json.toJson(this)
}
case class GQBattleTime(battle_time: String) extends InEventMessage
case class GQCharacterCreated(character_created: Boolean) extends InEventMessage {
	require(character_created == true, "Character Creation: Invalid message received")
}


object Event extends utils.CommonImplicits
object InEvent
object OutEvent
object ConnectionAlive

sealed trait Event {
  def toJson(): JsValue = Json.toJson(this)
}
case class InEvent(id: JsValue, input: JsValue) extends Event
case class OutEvent(id: JsValue, response: JsValue) extends Event
case class ConnectionAlive(message: String) extends Event {
	require(message == "connection_reset", "Connection Alive: Invalid message received")
}