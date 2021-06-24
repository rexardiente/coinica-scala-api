package utils.lib

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import com.typesafe.config.{ Config, ConfigFactory }
import scala.jdk.CollectionConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._
import utils.Config._
import models.domain.wallet.support.{ ETHJsonRpc, CoinCapAsset }

@Singleton
class MultiCurrencyHTTPSupport @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = NODE_SERVER_URI
  def getCoinCapAssets(): Future[Seq[CoinCapAsset]] = {
    val currencies: Seq[String] = SUPPORTED_CURRENCIES
    val request: WSRequest = ws.url(nodeServerURI + "/coincap/assets")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj("currencies" -> JsArray(currencies.map(JsString(_))))
    complexRequest
      .post(reqParams)
      .map(v => (v.json).as[Seq[CoinCapAsset]])
      .recover { case e: Exception => Seq.empty }
  }
  // prices will be base on coincap API...
  // https://coincap.io/assets/symbol
  def getCurrentPriceBasedOnMainCurrency(currency: String): Future[BigDecimal] = {
    for {
      coinCap <- getCoinCapAssets()
      process <- Future.successful {
        val selectedCurrency: Seq[CoinCapAsset] = coinCap.filter(_.symbol == currency)
        if (!selectedCurrency.isEmpty) selectedCurrency(0).priceUsd / coinCap(0).priceUsd
        else BigDecimal(0)
      }
    } yield (process)
  }
  def getETHTxInfo(txHash: String, currency: String): Future[Option[ETHJsonRpc]] = {
    val request: WSRequest = ws.url(nodeServerURI + "/etherscan/transaction/details")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj("tx_hash" -> txHash, "currency" -> currency)
    complexRequest
      .post(reqParams)
      .map(v => (v.json).asOpt[ETHJsonRpc])
      .recover { case e: Exception => None }
  }
  def walletWithdrawETH(id: UUID, address: String, amount: BigDecimal, fee: BigDecimal): Future[Option[Int]] = {
    val request: WSRequest = ws.url(nodeServerURI + "/wallet/withdraw-eth")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
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
  def walletWithdrawUSDC(id: UUID, address: String, amount: BigDecimal, fee: BigDecimal): Future[Option[Int]] = {
    val request: WSRequest = ws.url(nodeServerURI + "/wallet/withdraw-usdc")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
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