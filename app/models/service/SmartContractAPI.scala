package models.service

import javax.inject.{ Inject, Singleton }
import java.time.LocalDateTime
import java.util.{ Arrays, List, UUID }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import com.typesafe.config.{ Config, ConfigFactory}
import com.fasterxml.jackson.databind.ObjectMapper
import play.api.libs.ws._
import play.api.libs.json._
import io.jafka.jeos.{ EosApi, EosApiFactory }
import io.jafka.jeos.core.common.transaction.{ PackedTransaction, SignedPackedTransaction, TransactionAction }
import io.jafka.jeos.core.response.chain.Block
import io.jafka.jeos.core.response.chain.transaction.PushedTransaction
import io.jafka.jeos.impl.EosApiServiceGenerator
import io.jafka.jeos.exception.EosApiException
import models.domain.eosio.{ TableRowsRequest, GQRowsResponse }
import utils.lib.EOSIOSupport

@Singleton
class SmartContractAPI @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
	final val GQContractName  : String = "ghostquest"
	final val config          : Config = ConfigFactory.load()
  final val keosdApiBaseURL : String = config.getString("eosio.uri.keosd")
  final val nodeosApiBaseURL: String = config.getString("eosio.uri.nodeos")
  final val publicKey       : String = config.getString("eosio.wallets.public.default.key")
  final val privateKey      : String = config.getString("eosio.wallets.private.server.default.key")
  final val clientKeosdAPI  : EosApi = EosApiFactory.create(keosdApiBaseURL)
  final val clientNodeosAPI : EosApi = EosApiFactory.create(nodeosApiBaseURL)
  final val mapper          : ObjectMapper = EosApiServiceGenerator.getMapper()
	final val eosio 					: EOSIOSupport = new EOSIOSupport


	// ⑥ unlock the creator's wallet
  def unlockWalletAPI(): Either[EosApiException, Int] = 
    try {
      clientKeosdAPI.unlockWallet("default", privateKey)
      Right(1)
    } catch {
      case e: EosApiException => Left(e)
    }

  def lockAllWallets(): Either[EosApiException, Int] = 
    try {
      clientKeosdAPI.lockAllWallets()
      Right(1)
    } catch {
      case e: EosApiException => Left(e)
    }

	def battleAction(users: Seq[(String, String)], id: UUID): Future[Either[EosApiException, PushedTransaction]] = {
    // cleos convert pack_action_data ghostquest battle '{"username1":"user1", "ghost1_key":2, "username2":"user2", "ghost2_key":3}'
    val query: Seq[JsValue] = Seq(JsArray(Seq(JsArray(Seq(JsString(users(0)._1), JsString(users(0)._2))), JsArray(Seq(JsString(users(1)._1), JsString(users(1)._2))))), JsString(id.toString))

    eosio.abiJsonToBin(GQContractName, "battle", query).map { abiJsonToBinResult => 
      val actions: List[TransactionAction] = Arrays.asList(new TransactionAction(
          GQContractName,
          "battle",
          eosio.authorization(Seq(GQContractName)),
          abiJsonToBinResult.map(_.binargs.as[String]).getOrElse(null)))
      // track current block to avoid invalid ref block num
      val currentBlock: Block = eosio.getLatestBlock()
      val packedTx: PackedTransaction = new PackedTransaction()
          packedTx.setExpiration(LocalDateTime.parse(eosio.expirationInStr))
          packedTx.setRefBlockNum(currentBlock.getBlockNum())
          packedTx.setRefBlockPrefix(currentBlock.getRefBlockPrefix())
          packedTx.setDelaySec(0)
          packedTx.setMaxNetUsageWords(0)
          packedTx.setMaxCpuUsageMs(0)
          packedTx.setActions(actions)
      // check if transaction is successful
      try {
        val signedPackedTx: SignedPackedTransaction = eosio.signTransaction(
          packedTx,
          Seq(publicKey),
          clientNodeosAPI.getChainInfo().getChainId())

        Right(clientNodeosAPI.pushTransaction(null, signedPackedTx))
      }
      catch { case e: EosApiException => Left(e) }
    }
  }

  // TODO: Please check if character exists on users table...
  def removeCharacter(player: String, characterKey: String): Future[Either[EosApiException, PushedTransaction]] = {
  	val query = Seq(JsString(player), JsString(characterKey))

    eosio.abiJsonToBin(GQContractName, "eliminate", query).map { abiJsonToBinResult => 
      val actions: List[TransactionAction] = Arrays.asList(new TransactionAction(
          GQContractName,
          "eliminate",
          eosio.authorization(Seq(GQContractName)),
          abiJsonToBinResult.map(_.binargs.as[String]).getOrElse(null)))
      // track current block to avoid invalid ref block num
      val currentBlock: Block = eosio.getLatestBlock()
      val packedTx: PackedTransaction = new PackedTransaction()
          packedTx.setExpiration(LocalDateTime.parse(eosio.expirationInStr))
          packedTx.setRefBlockNum(currentBlock.getBlockNum())
          packedTx.setRefBlockPrefix(currentBlock.getRefBlockPrefix())
          packedTx.setDelaySec(0)
          packedTx.setMaxNetUsageWords(0)
          packedTx.setMaxCpuUsageMs(0)
          packedTx.setActions(actions)
      // check if transaction is successful
      try {
        val signedPackedTx: SignedPackedTransaction = eosio.signTransaction(
          packedTx,
          Seq(publicKey),
          clientNodeosAPI.getChainInfo().getChainId())

        Right(clientNodeosAPI.pushTransaction(null, signedPackedTx))
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
        "limit" -> 10 // set max result to 250 active users per request
      ))
      .map { response =>
         val validated: GQRowsResponse = (response.json).asOpt[GQRowsResponse].getOrElse(null)

         if (validated == null || validated.rows.size == 0) None
         else Some(validated)
      }
  }


}
