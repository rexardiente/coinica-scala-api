package models.domain

import play.api.libs.json._

object InEventMessage extends utils.CommonImplicits
object GQBattleTime
object GQCharacterCreated
object GQGetNextBattle
object VIPWSRequest
object EOSNotifyTransaction

sealed trait InEventMessage {
  def toJson(): JsValue = Json.toJson(this)
}
case class GQBattleTime(battle_time: String) extends InEventMessage
case class GQCharacterCreated(character_created: Boolean) extends InEventMessage {
	require(character_created == true, "Character Creation: invalid request")
}
case class GQGetNextBattle(GQ_NEXT_BATTLE: String) extends InEventMessage {
	require(GQ_NEXT_BATTLE == "get", "GQ Next Battle: invalid request")
}
case class EOSNotifyTransaction(EOS_NET_TRANSACTION: JsValue) extends InEventMessage {
	// require(GQ_NEXT_BATTLE == "get", "GQ Next Battle: invalid request")
}

// VIP objects
case class VIPWSRequest(user: String, command: String, request: String) extends InEventMessage {
	require(command == "vip", "VIP Command: invalid request")
	require(!akka.WebSocketActor.subscribers.filter(x => x._1 == user).isEmpty, "VIP: invalid request")
	require(
		request == "info" ||
		request == "current_rank" ||
		request == "payout" ||
		request == "point" ||
		request == "next_rank",
		"VIP Request: invalid request")
}

object Event extends utils.CommonImplicits
object InEvent extends utils.CommonImplicits
object OutEvent extends utils.CommonImplicits
object Subscribe extends utils.CommonImplicits
object ConnectionAlive extends utils.CommonImplicits

sealed trait Event {
  def toJson(): JsValue = Json.toJson(this)
}
case class InEvent(id: JsValue, input: JsValue) extends Event
case class OutEvent(id: JsValue, response: JsValue) extends Event
case class Subscribe(id: String, message: String) extends Event {
	require(message == "subscribe", "Subscribe: Invalid message received")
}
case class ConnectionAlive(message: String) extends Event {
	require(message == "connection_reset", "Connection Alive: Invalid message received")
}
