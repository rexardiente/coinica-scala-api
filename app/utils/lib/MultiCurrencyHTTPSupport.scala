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
  def getETHTxInfo(id: UUID, txType: String, txHash: String, currency: String): Future[Option[ETHJsonRpc]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/etherscan/transaction/details")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj(
      "account_id" -> id,
      "tx_type" -> txType,
      "tx_hash" -> txHash,
      "currency" -> currency)
    complexRequest
      .post(reqParams)
      .map(v => (v.json).asOpt[ETHJsonRpc])
      .recover { case e: Exception => None }
  }

  // def walletWithdrawETH(address: String, amount: Double, fee: Double): Future[Option[String]] = {
  //   val request: WSRequest = ws.url(nodeServerURI +  "/wallet/withdraw-eth")
  //   val complexRequest: WSRequest = request
  //     .addHttpHeaders("Accept" -> "application/json")
  //     .withRequestTimeout(10000.millis)
  //   val reqParams: JsValue = Json.obj(
  //     "address" -> address,
  //     "value" -> amount,
  //     "gasPrice" -> fee)
  //   complexRequest
  //     .post(reqParams)
  //     .map { v =>
  //       if (!(v.json \ "error").as[Boolean]) (v.json \ "tx_hash").asOpt[String]
  //       else None
  //     }
  //     .recover { case e: Exception => None }
  // }
  def walletWithdrawUSDC(address: String, amount: Double, fee: Double): Future[Option[Int]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/wallet/withdraw-usdc")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj(
      "address" -> address,
      "value" -> amount,
      "gasPrice" -> fee)
    complexRequest
      .post(reqParams)
      .map(_.json.asOpt[Int])
      .recover { case e: Exception => None }
  }
  // ===============================

  // route: /multi-currency/v1/coins
  // parameters: ""
  def getSupportedCoins(): Future[Seq[Coin]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/coins")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest
      .post("")
      .map(v => (v.json).as[Seq[Coin]])
      .recover { case e: Exception => Seq.empty }
  }
  // route: /multi-currency/v1/supported-pairs
  // parameters: {
  //   "coin_symbol": <symbol from get coins response>
  // }
  def getSupportedPairs(symbol: String): Future[Seq[Coin]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/supported-pairs")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj("coin_symbol" -> symbol)

    complexRequest
      .post(reqParams)
      .map(v => (v.json).as[Seq[Coin]])
      .recover { case e: Exception => Seq.empty }
  }
  // method: 'GET'
  // route: /multi-currency/v1/supported-deposit-coins
  // parameters: {
  //   "coin_symbol": <symbol from get coins response>
  // }
  def getSupportedDepositCoins(symbol: String): Future[Seq[Coin]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/supported-deposit-coins")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj("coin_symbol" -> symbol)

    complexRequest
      .post(reqParams)
      .map(v => (v.json).as[Seq[Coin]])
      .recover { case e: Exception => Seq.empty }
  }
  // route: /multi-currency/v1/exchange-limit
  // parameters: {
  //   "depositCoin": "btc",
  //   "destinationCoin": "eth"
  // }
  def getExchangeLimits(deposit: String, destination: String): Future[Option[Limits]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/exchange-limit")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj(
      "depositCoin" -> deposit,
      "destinationCoin" -> destination)

    complexRequest
      .post(reqParams)
      .map(v => (v.json).asOpt[Limits])
      .recover { case e: Exception => None }
  }
  // route: /multi-currency/v1/generate-offer
  // parameters: {
  //   "depositCoin": "btc",
  //   "destinationCoin": "eth"
  //   "depositCoinAmount": 0.0892 => should be in range from depositCoinMinAmount & depositCoinMaxAmount value from LIMITS api
  // }
  def generateOffer(deposit: String, destination: String, amount: BigDecimal): Future[Option[GenerateOffer]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/generate-offer")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj(
      "depositCoin" -> deposit,
      "destinationCoin" -> destination,
      "depositCoinAmount" -> amount)

    complexRequest
      .post(reqParams)
      .map(v => (v.json).asOpt[GenerateOffer])
      .recover { case e: Exception => None }
  }
  // route: /multi-currency/v1/make-order
  // parameters: {
  //   "transaction": {
  //     "depositCoin": "btc", => (string, required)
  //     "destinationCoin": "eth", => (string, required)
  //     "depositCoinAmount": 1, => (number, required)
  //     "destinationAddress": {
  //       "address": "", => (string, required)
  //       "tag": "", => (string, optional)
  //     },
  //     "refundAddress": {
  //       "address": "", => (string, required)
  //       "tag": "", => (string, optional)
  //     },
  //     "userReferenceId": "", => (string, required)
  //     "offerReferenceId": "" => (string, required)
  //   }
  // }
  def createOrder(order: CreateOrder): Future[Option[CreateOrderResponse]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/make-order")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest
      .post(order.toJson)
      .map(v => (v.json).asOpt[CreateOrderResponse])
      .recover { case e: Exception => None }
  }
  // route: /multi-currency/v1/order
  // parameters: {
  //   "orderId": "11111111-6c9e-4c53-9a6d-55e089aebd04"
  // }
  def getOrderStatus(id: String): Future[Option[OrderStatus]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/order")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj("orderId" -> id)

    complexRequest
      .post(reqParams)
      .map(v => (v.json).asOpt[OrderStatus])
      .recover { case e: Exception => None }
  }
  // route: /multi-currency/v1/orders
  // parameters: {
  //   "start": 0 <int, optional, default = 0>
  //   "count": 25 <int, optional, default = 25>
  //   "userRefId": "" <string, optional> (Optional parameter to filter orders for a userReferenceId)
  // }
  def getListOfOrders(start: Option[Int], count: Option[Int], id: Option[String]): Future[Option[ListOfOrders]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/orders")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj(
      "start" -> start.orElse(Some(1)),
      "count" -> count.orElse(Some(25)),
      "userRefId" -> id.getOrElse(null))

    complexRequest
      .post(reqParams)
      .map(v => (v.json).asOpt[ListOfOrders])
      .recover { case e: Exception => None }
  }
  // route: /multi-currency/v1/generate_keypairs
  // parameters: {
  //   "coin": "btc" (AVALIABLE COIN GENERATORS: "btc", "eth", "usdc")
  // }
  def generateKeyPairs(coin: String): Future[Option[KeyPairGeneratorResponse]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/multi-currency/v1/generate_keypairs")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    val reqParams: JsValue = Json.obj("coin" -> coin)

    complexRequest
      .post(reqParams)
      .map(v => (v.json).asOpt[KeyPairGeneratorResponse])
      .recover { case e: Exception => None }
  }
}