package utils.lib

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import com.typesafe.config.{ Config, ConfigFactory }
import scala.jdk.CollectionConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._
import models.domain.multi.currency._
import models.domain.wallet.support.ETHJsonRpc

@Singleton
class MultiCurrencyHTTPSupport @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = utils.Config.NODE_SERVER_URI

  // Version 2 Currency Support..
  def getETHTxInfo(txHash: String, currency: String): Future[Option[ETHJsonRpc]] = {
    val request: WSRequest = ws.url(nodeServerURI + "/etherscan/transaction/details")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(30000.millis)
    val reqParams: JsValue = Json.obj("tx_hash" -> txHash, "currency" -> currency)
    complexRequest
      .post(reqParams)
      .map(v => (v.json).asOpt[ETHJsonRpc])
      .recover { case e: Exception => None }
  }

  def walletWithdrawETH(id: UUID, address: String, amount: Double, fee: Double): Future[Option[Int]] = {
    val request: WSRequest = ws.url(nodeServerURI + "/wallet/withdraw-eth")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(30000.millis)
    val reqParams: JsValue = Json.obj(
      "account_id" -> id,
      "address" -> address,
      "value" -> amount,
      "gasPrice" -> fee)
    complexRequest
      .post(reqParams)
      .map(v => (v.json \ "status").asOpt[Int])
      .recover { case e: Exception => None }
  }
  def walletWithdrawUSDC(id: UUID, address: String, amount: Double, fee: Double): Future[Option[Int]] = {
    val request: WSRequest = ws.url(nodeServerURI + "/wallet/withdraw-usdc")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(30000.millis)
    val reqParams: JsValue = Json.obj(
      "account_id" -> id,
      "address" -> address,
      "value" -> amount,
      "gasPrice" -> fee)
    complexRequest
      .post(reqParams)
      .map(v => (v.json \ "status").asOpt[Int])
      .recover { case e: Exception => None }
  }
}