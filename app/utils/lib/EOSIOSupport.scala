package utils.lib

import javax.inject.{ Inject, Singleton }
import java.time.{ ZoneId, ZonedDateTime, LocalDateTime }
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{ Arrays, List }
import com.fasterxml.jackson.databind.ObjectMapper
import io.jafka.jeos.{ EosApi, EosApiFactory }
import io.jafka.jeos.core.common.transaction.{ PackedTransaction, SignedPackedTransaction, TransactionAction, TransactionAuthorization }
import io.jafka.jeos.core.request.chain.json2bin.TransferArg
import io.jafka.jeos.core.response.chain.{ AbiJsonToBin, Block }
import io.jafka.jeos.core.response.chain.transaction.PushedTransaction
import io.jafka.jeos.exception.EosApiException
import io.jafka.jeos.impl.{ EosApiServiceGenerator, EosApiRestClientImpl }

// import io.jafka.jeos.LocalApi
import com.typesafe.config.{ Config, ConfigFactory}

@Singleton
class EOSIOSupport {
  val config            : Config = ConfigFactory.load()
  val keosdApiBaseURL   : String = config.getString("eosio.uri.keosd")
  val nodeosApiBaseURL  : String = config.getString("eosio.uri.nodeos")
  val publicKey         : String = config.getString("eosio.wallets.public.default.key")
  val privateKey        : String = config.getString("eosio.wallets.private.default.key")
  val chainID           : String = config.getString("eosio.chain.id")
  // println(privateKey)
  // val from              : String = "treasurehunt" // sender
  // val to                : String = "user13" // sender
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
  private def authorization(acc: String): List[TransactionAuthorization] =
    Arrays.asList(new TransactionAuthorization(acc, "active"))

  // ④ build the all actions
  private def buildActions(auth: List[TransactionAuthorization], data: String) = 
    Arrays.asList(new TransactionAction("eosio.token", "transfer", auth, data))
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
      packedTx.setActions(buildActions(authorization(acc), data.getBinargs))

    packedTx
  }

  // ⑦ sign the transaction
  private def signTransaction(acc: String, data: AbiJsonToBin, block: Block): SignedPackedTransaction = 
    clientKeosdAPI.signTransaction(
      packedTransaction(acc, block, authorization(acc), data), 
      Arrays.asList(publicKey), 
      chainID)
  // println("println("signedPackedTransaction" + mapper.writeValueAsString(signedPackedTransaction))

  // ⑧ push the signed transaction 
  // return 1 for success and 0 for failed tx..
  def pushSignedTx(signedTx: SignedPackedTransaction): PushedTransaction =
    EosApiFactory.create(nodeosApiBaseURL).pushTransaction(null, signedTx)

  // ⑥ unlock the creator's wallet
  def unlockWalletAPI(): Unit = try {
    clientKeosdAPI.unlockWallet("default", privateKey)
  } catch {
    case ex: EosApiException => println(ex) 
  }
  def lockAllWallets(): Unit = clientKeosdAPI.lockAllWallets()

  // return 1 success and 0 failed
  def transfer(from: String, to: String, amount: String): Unit =
    try {
      unlockWalletAPI()  // open wallet
      val transferArg: TransferArg = new TransferArg(from, to, amount, "Executed from Server API")
      val data: AbiJsonToBin = clientNodeosAPI.abiJsonToBin("eosio.token", "transfer", transferArg)
      val signedTx: SignedPackedTransaction = signTransaction(from, data, getLatestBlock)

      pushSignedTx(signedTx)
    } catch {
      case e: Throwable => println(e)
    } finally {
      lockAllWallets() // close after transaction is finished
    }
}