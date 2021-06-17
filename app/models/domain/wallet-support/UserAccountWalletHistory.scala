package models.domain.wallet.support

import play.api.libs.json._

object UserAccountWalletHistory extends utils.CommonImplicits
// txType=deposit/withdraw
case class UserAccountWalletHistory(txHash: String, id: java.util.UUID, txType: String, data: CryptoJsonRpcHistory) {
	require(txType == "DEPOSIT" || txType == "WITHDRAW", "invalid parameter")
}