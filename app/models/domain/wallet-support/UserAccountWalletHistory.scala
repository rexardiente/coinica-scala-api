package models.domain.wallet.support

import java.util.UUID
import java.time.Instant
import play.api.libs.json._

object UserAccountWalletHistory extends utils.CommonImplicits {
	val tupled = (apply: (String, UUID, String, String, CryptoJsonRpcHistory, Instant) => UserAccountWalletHistory).tupled
}
// txType=deposit/withdraw
case class UserAccountWalletHistory(txHash: String,
																		id: UUID,
																		currency: String,
																		txType: String,
																		data: CryptoJsonRpcHistory,
																		createdAt: Instant) {
	require(txType == "DEPOSIT" || txType == "WITHDRAW", "invalid parameter")
}