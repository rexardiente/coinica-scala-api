package models.domain

import java.util.UUID
import play.api.libs.json._

object InEventMessage extends utils.CommonImplicits
object GQBattleTime
object GQCharacterCreated
object GQGetNextBattle
object VIPWSRequest
object EOSNotifyTransaction
object THGameResult

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

case class THPanelSet(key: Int, isopen: Int, iswin: Int) extends utils.CommonImplicits
case class THGameData(destination: Int,
											enemy_count: Int,
											maxprize: Double,
											nextprize: Double,
											odds: Double,
											panel_set: Seq[THPanelSet],
											prize: Double,
											status: Int,
											unopentile: Int,
											win_count: Int) extends utils.CommonImplicits
case class THGameResult(tx_hash: String, game_id: String, data: THGameData) extends InEventMessage
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
object ETHWalletTxEvent extends utils.CommonImplicits
object InEvent extends utils.CommonImplicits
object OutEvent extends utils.CommonImplicits
object Subscribe extends utils.CommonImplicits
object ConnectionAlive extends utils.CommonImplicits

sealed trait Event {
  def toJson(): JsValue = Json.toJson(this)
}
case class ETHWalletTxEvent(id: UUID,
													tx_hash: String,
													tx_type: String,
													data: models.domain.wallet.support.ETHJsonRpc) extends Event {
	require(tx_type == "DEPOSIT" || tx_type == "WITHDRAW", "invalid parameter")
}
case class InEvent(id: JsValue, input: InEventMessage) extends Event
case class OutEvent(id: JsValue, response: JsValue) extends Event
case class Subscribe(id: String, message: String) extends Event {
	require(message == "subscribe", "Subscribe: Invalid message received")
}
case class ConnectionAlive(message: String) extends Event {
	require(message == "connection_reset", "Connection Alive: Invalid message received")
}
