package utils.lib

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import scala.concurrent.duration._
// import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
// import play.api.http.HttpEntity
// import akka.actor.ActorSystem
// import akka.stream.scaladsl._
// import akka.util.ByteString
import scala.concurrent.ExecutionContext

import java.time.{ ZoneId, ZonedDateTime, LocalDateTime }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{ Arrays, HashMap, List, Map }
import org.apache.log4j.BasicConfigurator
import com.fasterxml.jackson.databind.ObjectMapper
import io.jafka.jeos.{ EosApi, EosApiFactory}
import io.jafka.jeos.core.common.transaction.{ PackedTransaction, SignedPackedTransaction, TransactionAction, TransactionAuthorization }
import io.jafka.jeos.core.request.chain.json2bin.TransferArg
import io.jafka.jeos.core.response.chain.{ AbiJsonToBin, Block }
import io.jafka.jeos.core.response.chain.transaction.PushedTransaction
import io.jafka.jeos.exception.EosApiException
import io.jafka.jeos.impl.{ EosApiServiceGenerator, EosApiRestClientImpl}

import io.jafka.jeos.LocalApi

@Singleton
class EOSIOSupport@Inject()(ws: WSClient)(implicit ec: ExecutionContext) {
  // println("Running EOSIOSupport!!!")
  // BasicConfigurator.configure()
  // Create Public and Private Keys
  // val api: LocalApi = EosApiFactory.createLocalApi()
  // val privateKey: String = api.createPrivateKey()
  // val publicKey: String = api.toPublicKey(privateKey)
  // println(publicKey + " " + privateKey)

  val walletApiBaseURL: String = "http://127.0.0.1:8900"
  val chainApiBaseURL: String = "http://127.0.0.1:8888"
  val from: String = "treasurehunt" // sender
  val to: String = "user2" // sender
  val clientWalletAPI: EosApi = EosApiFactory.create(walletApiBaseURL)
  val clientChainAPI: EosApi = EosApiFactory.create(chainApiBaseURL)

  val mapper: ObjectMapper = EosApiServiceGenerator.getMapper()

  // ① pack transfer data
  val transferArg: TransferArg = new TransferArg(from, to, "1.0000 EOS", "Executed from EOSIOSupport")
  val data: AbiJsonToBin = clientChainAPI.abiJsonToBin("eosio.token", "transfer", transferArg)
  // println("bin= " + data.getBinargs())

  // ② get the latest block info
  private def getLatestBlock() = {
    clientChainAPI.getBlock(clientChainAPI.getChainInfo().getHeadBlockId())
    // println("blockNum=" + block.getBlockNum())
    // val block: Block = 
  }
  // ③ create the authorization
  private def authorization() = {
    Arrays.asList(new TransactionAuthorization(from, "active"))
    // val authorizations: List[TransactionAuthorization] = 
  }
  // ④ build the all actions
  private def buildActions() = {
    Arrays.asList(new TransactionAction("eosio.token", "transfer", authorization(), data.getBinargs()))
    // returns actions: List[TransactionAction]
  }

  // ⑤ build the packed transaction
  def packedTransaction(): PackedTransaction = {
    // expired after 3 minutes
    val expiration: String = ZonedDateTime.now(ZoneId.of("GMT")).plusMinutes(3).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val packedTx: PackedTransaction = new PackedTransaction()
      packedTx.setRefBlockPrefix(getLatestBlock().getRefBlockPrefix())
      packedTx.setRefBlockNum(getLatestBlock().getBlockNum())
      packedTx.setExpiration(LocalDateTime.parse(expiration))
      packedTx.setRegion("0")
      packedTx.setMaxNetUsageWords(0)
      packedTx.setMaxCpuUsageMs(0)
      packedTx.setActions(buildActions())

    packedTx
  }

  // ⑥ unlock the creator's wallet
  def unlockWalletAPI(): Boolean = {
    try {
        clientWalletAPI.unlockWallet("default", "PW5J9srswAQKDMAhCKHer5pdKczsq5heMafi9DSqT28YrgvuDXApV")
        true
    } catch {
        case ex: EosApiException => 
          println(ex) 
          false
    }
  }

  // ⑦ sign the transaction
  def signTransaction(): SignedPackedTransaction = clientWalletAPI.signTransaction(
      packedTransaction,
      Arrays.asList("EOS6MRyAjQq8ud7hVNYcfnVPJqcVpscN5So8BhtHuGYqET5GDW5CV"),
      "8a34ec7df1b8cd06ff4a8abbaa7cc50300823350cadc59ab296cb00d104d2b8f")

    // println("println("signedPackedTransaction" + mapper.writeValueAsString(signedPackedTransaction))
    // println("\n--------------------------------\n")

  // ⑧ push the signed transaction
  def pushSignedTx(): Unit = try {
    EosApiFactory.create("http://127.0.0.1:8888").pushTransaction("none", signTransaction())
    // println("pushedTransaction=" + mapper.writeValueAsString(pushedTransaction))
  } catch {
    case e: Throwable => println(e)
  }

  def lockAllWallets(): Unit = clientWalletAPI.lockAllWallets()

  def unlockWalletPlay(): Unit = {
    val queryURL: String = walletApiBaseURL + "/v1/wallet/unlock"
    // send_transactions
    val body = JsArray(Seq(JsString("default"), JsString("PW5J9srswAQKDMAhCKHer5pdKczsq5heMafi9DSqT28YrgvuDXApV")))
    val request: WSRequest = ws.url(queryURL)
    val complexRequest: WSRequest =
        request
          .addHttpHeaders("Accept" -> "application/json")
          // .addQueryStringParameters("search" -> "play")
          .withRequestTimeout(10000.millis)

    val futureResponse: Future[WSResponse] = complexRequest.post(body)

    futureResponse.map { response =>
      if (response.status == 200) {
        println(response.body)
      }
      else
        println(response.body)
    }
  }

  unlockWalletAPI() // check if wallet is already opened
  pushSignedTx()
  lockAllWallets() // lock all wallets after execution
}