package models.domain.wallet.support

import play.api.libs.json._

object CryptoJsonRpc extends utils.CommonImplicits
object ETHJsonRpcResult extends utils.CommonImplicits
object ETHJsonRpc
object CryptoJsonRpcHistory extends utils.CommonImplicits

sealed trait CryptoJsonRpc {
	def toJson(): JsValue = Json.toJson(this)
}
sealed trait CryptoJsonRpcHistory {
	def toJson(): JsValue = Json.toJson(this)
}
case class ETHJsonRpcResult(blockHash: String,
											      blockNumber: Long,
											      from: String,
											      gas: Long,
											      gasPrice: Int,
											      hash: String,
											      input: String,
											      nonce: Int,
											      to: String,
											      transactionIndex: String,
											      value: String,
											      `type`: String,
											      v: String,
											      r: String,
											      s: String) extends CryptoJsonRpcHistory
case class ETHJsonRpc(currency: String, jsonrpc: String, id: Int, result: ETHJsonRpcResult) extends CryptoJsonRpc {
	override def toJson(): JsValue = Json.toJson(this)
}

