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
class GQSmartContractAPI @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
	private val support: EOSIOSupport = new EOSIOSupport()
	private val GQContractName: String = "ghostquest"

  private def defaultThreadSleep(): Unit = Thread.sleep(1000)

	def battleAction(users: Seq[(String, String)], id: UUID): Future[Option[PushedTransaction]] = {
    support.unlockWalletAPI()
    defaultThreadSleep()
    // cleos convert pack_action_data ghostquest battle '{"username1":"user1", "ghost1_key":2, "username2":"user2", "ghost2_key":3}'
    val query: Seq[JsValue] = Seq(JsArray(Seq(JsArray(Seq(JsString(users(0)._1), JsString(users(0)._2))), JsArray(Seq(JsString(users(1)._1), JsString(users(1)._2))))), JsString(id.toString))

    support.abiJsonToBin(GQContractName, "battle", query).map { abiJsonToBinResult =>
      val actions: List[TransactionAction] = Arrays.asList(new TransactionAction(
          GQContractName,
          "battle",
          support.authorization(Seq(GQContractName)),
          abiJsonToBinResult.map(_.binargs.as[String]).getOrElse(null)))
      // track current block to avoid invalid ref block num
      val currentBlock: Block = support.getLatestBlock()
      val packedTx: PackedTransaction = new PackedTransaction()
          packedTx.setExpiration(LocalDateTime.parse(support.expirationInStr))
          packedTx.setRefBlockNum(currentBlock.getBlockNum())
          packedTx.setRefBlockPrefix(currentBlock.getRefBlockPrefix())
          packedTx.setDelaySec(0)
          packedTx.setMaxNetUsageWords(0)
          packedTx.setMaxCpuUsageMs(0)
          packedTx.setActions(actions)
      // check if transaction is successful
      val result = try {
        val signedPackedTx: SignedPackedTransaction = support.signTransaction(
            packedTx,
            Seq(support.publicKey),
            support.clientNodeosAPI.getChainInfo().getChainId())

        Some(support.clientNodeosAPI.pushTransaction(null, signedPackedTx))
      } catch { case e: EosApiException => None }

      support.lockAllWallets()
      result
    }
  }

  // TODO: Please check if character exists on users table...
  def removeCharacter(player: String, characterKey: String): Future[Option[PushedTransaction]] = {
    support.unlockWalletAPI()
  	val query = Seq(JsString(player), JsString(characterKey))

    support.abiJsonToBin(GQContractName, "eliminate", query).map { abiJsonToBinResult =>
      val actions: List[TransactionAction] = Arrays.asList(new TransactionAction(
          GQContractName,
          "eliminate",
          support.authorization(Seq(GQContractName)),
          abiJsonToBinResult.map(_.binargs.as[String]).getOrElse(null)))
      // track current block to avoid invalid ref block num
      val currentBlock: Block = support.getLatestBlock()
      val packedTx: PackedTransaction = new PackedTransaction()
          packedTx.setExpiration(LocalDateTime.parse(support.expirationInStr))
          packedTx.setRefBlockNum(currentBlock.getBlockNum())
          packedTx.setRefBlockPrefix(currentBlock.getRefBlockPrefix())
          packedTx.setDelaySec(0)
          packedTx.setMaxNetUsageWords(0)
          packedTx.setMaxCpuUsageMs(0)
          packedTx.setActions(actions)
      // check if transaction is successful
      val result = try {
        val signedPackedTx: SignedPackedTransaction = support.signTransaction(
          packedTx,
          Seq(support.publicKey),
          support.clientNodeosAPI.getChainInfo().getChainId())

        Some(support.clientNodeosAPI.pushTransaction(null, signedPackedTx))
      } catch { case e: EosApiException =>
      println(e)
      None }

      support.lockAllWallets()
      result
    }
  }

  def getGQUsers(req: TableRowsRequest): Future[Option[GQRowsResponse]] = {
    val request: WSRequest = ws.url(support.nodeosApiBaseURL + support.config.getString("eosio.uri.path.get_table_rows"))
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
