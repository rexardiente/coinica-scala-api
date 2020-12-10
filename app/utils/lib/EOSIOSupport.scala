package utils.lib

import javax.inject.{ Inject, Singleton }
import java.time.{ ZoneId, ZonedDateTime, LocalDateTime }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{ Arrays, List, ArrayList }
import scala.util._
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
import io.jafka.jeos.exception.EosApiException
import models.domain.eosio.{ BinaryArgs, TableRowsRequest, GQRowsResponse }

@Singleton
class EOSIOSupport @Inject()(ws: WSClient)(implicit ec: scala.concurrent.ExecutionContext) {
  val config            : Config = ConfigFactory.load()
  val keosdApiBaseURL   : String = config.getString("eosio.uri.keosd")
  val nodeosApiBaseURL  : String = config.getString("eosio.uri.nodeos")
  val publicKey         : String = config.getString("eosio.wallets.public.default.key")
  val privateKey        : String = config.getString("eosio.wallets.private.server.default.key")
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
  def unlockWalletAPI(): Either[EosApiException, Unit] = 
    try {
      Right(clientKeosdAPI.unlockWallet("default", privateKey))
    } catch {
      case e: EosApiException => Left(e)
    }

  def lockAllWallets(): Either[EosApiException, Unit] = 
    try {
      Right(clientKeosdAPI.lockAllWallets())
    } catch {
      case e: EosApiException => Left(e)
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

  // requires account name and characterID
  def battleAction(users: Seq[(String, Int)]): Future[Either[EosApiException, PushedTransaction]] = {
    // unlockWalletAPI()
    // cleos convert pack_action_data ghostquest battle '{"username1":"user1", "ghost1_key":2, "username2":"user2", "ghost2_key":3}'
    abiJsonToBin("ghostquest", "battle", Seq(users(0)._1, users(0)._2, users(1)._1, users(1)._2)).map { abiJsonToBinResult => 
      val actions: List[TransactionAction] = Arrays.asList(new TransactionAction(
          "ghostquest",
          "battle",
          authorization(Seq("ghostquest")),
          abiJsonToBinResult.map(_.binargs.as[String]).getOrElse(null)))
      // track current block to avoid invalid ref block num
      val currentBlock: Block = getLatestBlock()
      val packedTx: PackedTransaction = new PackedTransaction()
          packedTx.setExpiration(LocalDateTime.parse(expirationInStr))
          packedTx.setRefBlockNum(currentBlock.getBlockNum())
          packedTx.setRefBlockPrefix(currentBlock.getRefBlockPrefix())
          packedTx.setDelaySec(0)
          packedTx.setMaxNetUsageWords(0)
          packedTx.setMaxCpuUsageMs(0)
          packedTx.setActions(actions)
      // check if transaction is successful
      try {
        val signedPackedTx: SignedPackedTransaction = signTransaction(
          packedTx,
          Seq(publicKey),
          clientNodeosAPI.getChainInfo().getChainId())

        Right(clientNodeosAPI.pushTransaction(null, signedPackedTx))
        // mapper.writeValueAsString(pushedTransaction).toString
      }
      catch { case e: EosApiException => Left(e) }
    }
  }

  def getGQUsers(req: TableRowsRequest): Future[Option[GQRowsResponse]] = {
    val request: WSRequest = ws.url(nodeosApiBaseURL + config.getString("eosio.uri.path.get_table_rows"))
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest
      .post(Json.obj(
        "code" -> req.code,
        "table" -> req.table,
        "scope" -> req.scope,
        "index_position" -> req.index_position.getOrElse(null),
        "key_type" -> req.key_type.getOrElse(null),
        "encode_type" -> req.encode_type.getOrElse(null),
        "upper_bound" -> req.upper_bound.getOrElse(null),
        "lower_bound" -> req.lower_bound.getOrElse(null),
        "json" -> true, // add this to format result into JSON
        "limit" -> 250 // set max result to 250 active users per request
      ))
      .map { response =>
         val validated: GQRowsResponse = (response.json).asOpt[GQRowsResponse].getOrElse(null)

         if (validated == null || validated.rows.size == 0) None
         else Some(validated)
      }
  }

  private def expirationInStr(): String = {
    ZonedDateTime
      .now(ZoneId.of("GMT"))
      .plusMinutes(3)
      .truncatedTo(ChronoUnit.SECONDS)
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
  private def abiJsonToBin(code: String, action: String, args: Seq[Any]): Future[Option[BinaryArgs]] = {
    val request: WSRequest = ws.url(nodeosApiBaseURL + config.getString("eosio.uri.path.abi_json_to_bin"))
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    complexRequest
      .post(Json.obj("code" -> code, "action" -> action, "args" -> JsArray(args.map {
        case str: String => JsString(str)
        case num: Int => JsNumber(num)
        case bol: Boolean => JsBoolean(bol)
        case arr: JsArray => arr
        case any => JsNull(any.toString)
      })))
      .map(_.json.asOpt[BinaryArgs])
  }
}

// what mechanism is used to serialise the transaction JSON into packed_trx? chainId