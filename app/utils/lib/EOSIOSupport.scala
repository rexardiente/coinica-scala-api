package utils.lib

import javax.inject.{ Inject, Singleton }
import java.time.{ ZoneId, ZonedDateTime, LocalDateTime }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{ Arrays, List, ArrayList }
import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory}
import com.fasterxml.jackson.databind.ObjectMapper
import play.api.libs.ws._
import play.api.libs.json._
import io.jafka.jeos.{ EosApi, EosApiFactory }
import io.jafka.jeos.core.common.transaction.{ PackedTransaction, SignedPackedTransaction, TransactionAction, TransactionAuthorization }
// import io.jafka.jeos.core.request.chain.json2bin.TransferArg
import io.jafka.jeos.core.response.chain.{ AbiJsonToBin, Block }
import io.jafka.jeos.core.response.chain.transaction.PushedTransaction
import io.jafka.jeos.impl.EosApiServiceGenerator
import models.domain.eosio.{ BinaryArgs, TableRowsRequest }

@Singleton
class EOSIOSupport @Inject()(ws: WSClient)(implicit ec: scala.concurrent.ExecutionContext) {
  val config            : Config = ConfigFactory.load()
  val keosdApiBaseURL   : String = config.getString("eosio.uri.keosd")
  val nodeosApiBaseURL  : String = config.getString("eosio.uri.nodeos")
  val publicKey         : String = config.getString("eosio.wallets.public.default.key")
  val privateKey        : String = config.getString("eosio.wallets.private.default.key")
  val clientKeosdAPI    : EosApi = EosApiFactory.create(keosdApiBaseURL)
  val clientNodeosAPI   : EosApi = EosApiFactory.create(nodeosApiBaseURL)
  val mapper            : ObjectMapper = EosApiServiceGenerator.getMapper()
  
  // ② get the latest block info
  def getLatestBlock(): Block = {
    clientNodeosAPI.getBlock(clientNodeosAPI.getChainInfo().getHeadBlockId())
    // println("blockNum=" + block.getBlockNum())
    // val block: Block = 
  }

  // ③ create the authorization
  private def authorization(acc: Seq[String]): List[TransactionAuthorization] = 
    new ArrayList(acc.map(x => new TransactionAuthorization(x, "active")).asJavaCollection)

  // ④ build the all actions
  private def buildActions(contract: String, action: String, auth: List[TransactionAuthorization], data: String) = 
    Arrays.asList(new TransactionAction(contract, action, auth, data))
    // returns actions: List[TransactionAction]

  // ⑤ build the packed transaction
  private def packedTransaction(acc: String, block: Block, auth: List[TransactionAuthorization], data: AbiJsonToBin): PackedTransaction = {
    // expired after 3 minutes
    val expiration: String = ZonedDateTime.now(ZoneId.of("GMT")).plusMinutes(3).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val packedTx: PackedTransaction = new PackedTransaction()
      packedTx.setRefBlockPrefix(block.getRefBlockPrefix())
      packedTx.setRefBlockNum(block.getBlockNum())
      packedTx.setExpiration(LocalDateTime.parse(expiration))
      packedTx.setRegion("0")
      packedTx.setMaxNetUsageWords(0)
      packedTx.setMaxCpuUsageMs(0)
      packedTx.setActions(buildActions("eosio.token", "action", authorization(Seq(acc)), data.getBinargs))

    packedTx
  }

  // ⑦ sign the transaction
  private def signTransaction(packedTx: PackedTransaction, pubKeys: Seq[String], chainId: String): SignedPackedTransaction = 
    clientKeosdAPI.signTransaction(packedTx, new ArrayList(pubKeys.asJavaCollection), chainId)

  // ⑥ unlock the creator's wallet
  def unlockWalletAPI(): Unit = 
    try {
      clientKeosdAPI.unlockWallet("default", privateKey)
    } catch {
      case ex: Throwable => println(ex) 
    }

  def lockAllWallets(): Unit = 
    try {
      clientKeosdAPI.lockAllWallets()
    } catch {
      case ex: Throwable => println(ex) 
    }

  // ⑧ push the signed transaction 
  // return 1 for success and 0 for failed tx..
  // def pushSignedTx(signedTx: SignedPackedTransaction): PushedTransaction =
  //   EosApiFactory.create(nodeosApiBaseURL).pushTransaction(null, signedTx)

  // def transfer(from: String, to: String, amount: String): Unit =
  //   try {
  //     unlockWalletAPI()  // open wallet
  //     val transferArg: TransferArg = new TransferArg(from, to, amount, "Executed from Server API")
  //     val data: AbiJsonToBin = clientNodeosAPI.abiJsonToBin("eosio.token", "transfer", transferArg)
  //     val signedTx: SignedPackedTransaction = signTransaction(from, data, getLatestBlock)
  //     pushSignedTx(signedTx)
  //   } catch {
  //     case e: Throwable => println(e)
  //   } finally {
  //     lockAllWallets() // close after transaction is finished
  //   }

  def getAllUsers(): Unit = {
    val req = new TableRowsRequest("treasurehunt", "users", "treasurehunt", None, Some("uint64_t"), None, None, None)

    val request: WSRequest = ws.url(nodeosApiBaseURL + config.getString("eosio.uri.path.get_table_rows"))
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    // val js_response: JsValue = Json.parse("""{"rows":["00000000807015d60000000000000000100000000100000200000300000400000500000600000700000800000900000a00000b00000c00000d00000e00000f00001000010105102700000000000004454f530000000046175d74d145f73fae3700000000000004454f530000000000a415020000000004454f5300000000"],"more":false,"next_key":""}""")
    // val get_rows: Seq[String] = (js_response \ "rows").as[Seq[String]]
    // val row: String = get_rows(0)
    // get_rows.map(x => println(x))

    val sample_res: JsValue = Json.parse("""{
      "row": [{
        "username":"user1", 
        "game_id":0,
        "game_data":{
          "panel_set":[
            {"key":0, "isopen":0, "iswin":0},
            {"key":1, "isopen":0, "iswin":0},
            {"key":2, "isopen":0, "iswin":0},
            {"key":3, "isopen":0, "iswin":0},
            {"key":4, "isopen":0, "iswin":0},
            {"key":5, "isopen":0, "iswin":0},
            {"key":6, "isopen":0, "iswin":0},
            {"key":7, "isopen":1, "iswin":1},
            {"key":8, "isopen":0, "iswin":0},
            {"key":9, "isopen":0, "iswin":0},
            {"key":10, "isopen":0, "iswin":0},
            {"key":11, "isopen":0, "iswin":0},
            {"key":12, "isopen":0, "iswin":0},
            {"key":13, "isopen":0, "iswin":0},
            {"key":14, "isopen":0,"iswin":0},
            {"key":15,"isopen":0, "iswin":0}
          ],
          "unopentile":15,
          "win_count":1,
          "destination":1,
          "status":1,
          "enemy_count":1,
          "prize":"1.0453 EOS",
          "odds":"1.07142857142857140",
          "nextprize":"1.0975 EOS",
          "maxprize":"11.8125 EOS"
        }
      }],
      "more":false,
      "next_key":""
    }""")

    // abiBinToJson(row)
    complexRequest
      .post(Json.obj(
        "json" -> true, // add this to format result into JSON
        "code" -> req.code,
        "table" -> req.table,
        "scope" -> req.scope,
        "index_position" -> req.index_position.getOrElse(null),
        "key_type" -> req.key_type.getOrElse(null),
        "encode_type" -> req.encode_type.getOrElse(null),
        "upper_bound" -> req.upper_bound.getOrElse(null),
        "lower_bound" -> req.lower_bound.getOrElse(null)
      ))
      .map { response =>
         println(response.body)
         val rows: Seq[JsValue] = (response.json \ "rows").as[Seq[JsValue]]

         if (rows.size > 0) {
          Seq.empty
         } 
         else Seq.empty
      }
  }

  private def expirationInStr(): String = {
    ZonedDateTime
      .now(ZoneId.of("GMT"))
      .plusMinutes(3)
      .truncatedTo(ChronoUnit.SECONDS)
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
  }

  def hi(user: String): Unit = {
    try {
      // unlockWalletAPI()
      abiJsonToBin("hello", "hi", Seq(user)).map { data => 
        val actions: List[TransactionAction] = Arrays.asList(new TransactionAction(
            "hello",
            "hi",
            authorization(Seq(user)), 
            data.map(_.binargs.as[String]).getOrElse(null)))

        val packedTx: PackedTransaction = new PackedTransaction()
            packedTx.setExpiration(LocalDateTime.parse(expirationInStr))
            packedTx.setRefBlockNum(getLatestBlock.getBlockNum())
            packedTx.setRefBlockPrefix(getLatestBlock.getRefBlockPrefix())
            packedTx.setDelaySec(0)
            packedTx.setMaxNetUsageWords(0)
            packedTx.setMaxCpuUsageMs(0)
            packedTx.setActions(actions)

        val signedPackedTransaction: SignedPackedTransaction = 
            signTransaction(
              packedTx,
              Seq(publicKey),
              clientNodeosAPI.getChainInfo().getChainId())

        val pushedTransaction: PushedTransaction = clientNodeosAPI.pushTransaction(null, signedPackedTransaction)

        println(mapper.writeValueAsString(pushedTransaction))
      }
    } catch  {
      case e: Throwable => println(e)
    }
  }

  // sign trasanction in JSON response
  private def signTransactionToJson(args: Option[BinaryArgs]): Future[JsValue] = {
    val request: WSRequest = ws.url(keosdApiBaseURL + config.getString("eosio.uri.path.sign_transaction"))
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      // .withRequestTimeout(10000.millis)
    val expiration: String = ZonedDateTime
      .now(ZoneId.of("GMT"))
      .plusMinutes(3)
      .truncatedTo(ChronoUnit.SECONDS)
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    val data: JsValue = JsArray(Seq(
      Json.obj(
        "expiration" -> expiration,
        "ref_block_num" -> getLatestBlock.getBlockNum.toLong,
        "ref_block_prefix" -> getLatestBlock.getRefBlockPrefix.toLong,
        "transaction_extensions" -> JsArray.empty,
        "context_free_actions" -> JsArray.empty,
        "context_free_data" -> JsArray.empty,
        "delay_sec" -> 0,
        "max_cpu_usage_ms" -> 0,
        "max_net_usage_words" -> 0,
        "signatures" -> JsArray.empty,
        "actions" -> JsArray(Seq(Json.obj(
          "account" -> "hello",
          "name" -> "hi",
          "data" -> args.map(_.binargs).getOrElse(null),
          "authorization" -> JsArray(Seq(Json.obj(
            "actor" -> "user1",
            "permission" -> "active"
          )))
        )))
      ),
      JsArray(Seq(JsString(publicKey))),
      JsString(clientNodeosAPI.getChainInfo().getChainId())// chainID
    ))

    complexRequest.post(data).map(_.json)
  }

  // custom abi to json bin function..
  private def abiBinToJson(binargs: String): Unit = {
    val request: WSRequest = ws.url(nodeosApiBaseURL + config.getString("eosio.uri.path.abi_bin_to_json"))
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    complexRequest
      .post(Json.obj("binargs" -> binargs))
      .map(x => println(x))
  }

  // custom abi to json bin function..
  private def abiJsonToBin(code: String, action: String, args: Seq[String]): Future[Option[BinaryArgs]] = {
    val request: WSRequest = ws.url(nodeosApiBaseURL + config.getString("eosio.uri.path.abi_json_to_bin"))
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    complexRequest
      .post(Json.obj("code" -> code, "action" -> action, "args" -> JsArray(args.map(JsString(_)))))
      .map(_.json.asOpt[BinaryArgs])
  }
}

// what mechanism is used to serialise the transaction JSON into packed_trx? chainId