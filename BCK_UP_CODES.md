cleos convert pack_action_data hello hi '{"user":"user1"}'

cleos convert pack_transaction '{
  "expiration": "2020-11-27T03:52:45",
  "ref_block_num": 14207,
  "ref_block_prefix": 1438248607,
  "max_net_usage_words": 0,
  "max_cpu_usage_ms": 0,
  "delay_sec": 0,
  "context_free_actions": [],
  "actions": [{
      "account": "user1",
      "name": "hello",
      "authorization": [{
          "actor": "user1",
          "permission": "active"
        }
      ],
      "data": "00000000807015d6"
    }
  ],
  "transaction_extensions": []
}
cleos convert pack_transaction '{"expiration":"2020-11-30T12:56:57","ref_block_num":64186,"ref_block_prefix":2042891937,"max_net_usage_words":0,"max_cpu_usage_ms":0,"delay_sec":0,"context_free_actions":[],"actions":[{"account":"hello","name":"hi","authorization":[{"actor":"user1","permission":"active"}],"data":"00000000807015d6"}],"transaction_extensions":[],"signatures":["SIG_K1_JzCpYYnuPDfXqLJBgqVSnLbaGa96PgoGxkwww2MQQrmu5v6tSJBZMcQVr9UzBe5ah6hYohxARh44NsSuAN4LHhYDcLaZem"],"context_free_data":[]}'


{
  "signatures": [],
  "compression": "none",
  "packed_context_free_data": "",
  "packed_trx": "04f8c75f7f379feeb955000000000100000000807015d600000000001aa36a0100000000807015d600000000a8ed32320800000000807015d600"
}



def transfer(from: String, to: String, amount: String): Unit =
try {
  // unlockWalletAPI()  // open wallet
  // val transferArg: TransferArg = new TransferArg(from, to, amount, "Executed from Server API")
  // val data: AbiJsonToBin = clientNodeosAPI.abiJsonToBin("eosio.token", "transfer", transferArg)

  abi_json_to_bin.map { bin =>
    val abiJson = new AbiJsonToBin()
    abiJson.setBinargs(bin.get.binargs.as[String])

    // val signedTx: SignedPackedTransaction = signTransaction("user1", abiJson, getLatestBlock)

    val expiration: String = ZonedDateTime.now(ZoneId.of("GMT")).plusMinutes(3).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    var packedTx: PackedTransaction = new PackedTransaction()
    packedTx.setRefBlockPrefix(getLatestBlock.getRefBlockPrefix())
    packedTx.setRefBlockNum(getLatestBlock.getBlockNum())
    packedTx.setExpiration(LocalDateTime.parse(expiration))
    packedTx.setRegion("0")
    packedTx.setMaxNetUsageWords(0)
    packedTx.setMaxCpuUsageMs(0)
    packedTx.setActions(buildActions("hello", "action", authorization("user1"), abiJson.getBinargs))

    val signedTx: SignedPackedTransaction = clientKeosdAPI.signTransaction(
      packedTx, 
      Arrays.asList("EOS6MRyAjQq8ud7hVNYcfnVPJqcVpscN5So8BhtHuGYqET5GDW5CV"), 
      chainID)

    pushSignedTx(signedTx)
  }
  
} catch {
  case e => println(e)
} 

// val signedTx: JsValue = tx
    // val signature: String = signedTx.signatures.value(0).as[String]
    // val signature: Seq[String] = (signed \ "signatures").get.as[Seq[String]] // convert signed to packed        

    // val packedTx: PackedTransaction = new PackedTransaction()
    // val expiration: String = ZonedDateTime
    //   .now(ZoneId.of("GMT"))
    //   .plusMinutes(3)
    //   .truncatedTo(ChronoUnit.SECONDS)
    //   .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    // packedTx.setRefBlockPrefix(getLatestBlock.getRefBlockPrefix())
    // packedTx.setRefBlockNum(getLatestBlock.getBlockNum())
    // packedTx.setExpiration(LocalDateTime.parse(expiration))
    // packedTx.setRegion("0")
    // packedTx.setMaxNetUsageWords(0)
    // packedTx.setMaxCpuUsageMs(0)
    // packedTx.setActions(buildActions("hello", "hi", authorization("user1"), abiJsonToBin.get.binargs.as[String]))

    // val raw: Raw = new Raw()
    // //chain
    // raw.pack(Hex.toBytes(chainID))
    // //expiration
    // raw.packUint32(packedTx.getExpiration().toEpochSecond(java.time.ZoneOffset.ofHours(0)))
    // //ref_block_num
    // raw.packUint16(packedTx.getRefBlockNum().intValue())
    // //ref_block_prefix
    // raw.packUint32(packedTx.getRefBlockPrefix())
    // //max_net_usage_words
    // raw.packVarint32(packedTx.getMaxNetUsageWords().toLong)
    // //max_cpu_usage_ms
    // raw.packUint8(packedTx.getMaxCpuUsageMs())//TODO: what the type?
    // //delay_sec
    // raw.packVarint32(packedTx.getDelaySec().toLong)
    // //context_free_actions
    // raw.packVarint32(packedTx.getContextFreeActions().size())

    // val raw: Raw = new Raw()
    // raw.packName("user1")
    // raw.toHex()

    //  val push_data: JsValue = Json.obj(
    //   "signatures" -> JsArray(Seq( // generated signature after pack_transaction
    //       JsString("SIG_K1_Ke2iXHrDFktim119DfzhNCpEHAc8ZRx1McCPfgEdpPmcGSVvidTTHschnQQas1tWjKcZzQwVSczbeMADN7h6U6oaGwBCzD")
    //   )), 
    //   "compression" -> false,
    //   "packed_context_free_data" -> JsNull,
    //   "packed_trx" -> raw.toString
    // )


// val data: JsValue = JsArray(Seq(
//   ToSignedTransaction(
//     expiration,
//     getLatestBlock.getBlockNum.toLong,
//     getLatestBlock.getRefBlockPrefix.toLong,
//     0,
//     0,
//     0,
//     JsArray.empty,
//     JsArray(Seq(
//       Json.obj(
//         "account" -> "hello",
//         "name" -> "hi",
//         "authorization" -> JsArray(Seq(Json.obj("actor" -> "user1", "permission" -> "active")))
//       )))).toJson,
//   JsArray(Seq(JsString("EOS6MRyAjQq8ud7hVNYcfnVPJqcVpscN5So8BhtHuGYqET5GDW5CV"))),
//   args.map(_.binargs).get
// ))




// for {
//   abiJsonToBin <- abi_json_to_bin()
//   signTx       <- sign_transaction(abiJsonToBin)
// } yield signTx match {
//   case signed: JsValue =>
//     val expiration: String = ZonedDateTime
//     .now(ZoneId.of("GMT"))
//     .plusMinutes(3)
//     .truncatedTo(ChronoUnit.SECONDS)
//     .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
//     // val signedJS = """{"expiration":"2020-11-30T12:56:57","ref_block_num":64186,"ref_block_prefix":2042891937,"max_net_usage_words":0,"max_cpu_usage_ms":0,"delay_sec":0,"context_free_actions":[],"actions":[{"account":"hello","name":"hi","authorization":[{"actor":"user1","permission":"active"}],"data":"00000000807015d6"}],"transaction_extensions":[],"signatures":["SIG_K1_JzCpYYnuPDfXqLJBgqVSnLbaGa96PgoGxkwww2MQQrmu5v6tSJBZMcQVr9UzBe5ah6hYohxARh44NsSuAN4LHhYDcLaZem"],"context_free_data":[]}"""

//     // ① pack transfer data
//     val hiData: String = packHello("user1")
//     val authorizations: List[TransactionAuthorization] = Arrays.asList(new TransactionAuthorization("user1", "active"))
    
//     // ④ build the all actions
//     val actions: List[TransactionAction] = Arrays.asList(new TransactionAction("hello", "hi", authorizations, hiData))

//     var packedTx: PackedTransaction = new PackedTransaction()
//     packedTx.setExpiration(LocalDateTime.parse(expiration))
//     packedTx.setRefBlockNum(getLatestBlock.getBlockNum())
//     packedTx.setRefBlockPrefix(getLatestBlock.getRefBlockPrefix())
//     packedTx.setDelaySec(0)
//     packedTx.setMaxNetUsageWords(0)
//     packedTx.setMaxCpuUsageMs(0)
//     packedTx.setActions(actions)

//     val signedPackedTransaction: SignedPackedTransaction = clientKeosdAPI.signTransaction(packedTx, //
//             Arrays.asList("EOS6MRyAjQq8ud7hVNYcfnVPJqcVpscN5So8BhtHuGYqET5GDW5CV"), //
//             clientNodeosAPI.getChainInfo().getChainId())

//     // val pushedTransaction: PushedTransaction = clientKeosdAPI.pushTransaction("none", signedPackedTransaction)
//     // println("pushedTransaction=" + mapper.writeValueAsString(pushedTransaction))
//     // val arg: SignArg = clientNodeosAPI.getSignArg(120)
//     // new io.jafka.jeos.impl.LocalApiImpl().sign(privateKey, arg, packedTx)

//     // val hash: String = sign(privateKey, 
//         // arg,
//         // CustomSignArg(
//         //   clientNodeosAPI.getChainInfo().getHeadBlockNum(),
//         //   clientNodeosAPI.getChainInfo().getLastIrreversibleBlockNum(),
//         //   getLatestBlock.getRefBlockPrefix(),
//         //   LocalDateTime.parse(expiration),
//         //   clientNodeosAPI.getChainInfo().getChainId(),
//         //   1
//         //   // clientNodeosAPI.getChainInfo().getHeadBlockTime().atZone(ZoneId.of("GMT")).toInstant().toEpochMilli().toInt
//         // ), 
//         // packedTx)
//     // val req: PushTransactionRequest = new PushTransactionRequest()
//     // req.setTransaction(packedTx)
//     try {
//       clientNodeosAPI.pushTransaction("none", signedPackedTransaction)
//     } catch {
//       case e: Throwable => println(e)
//     }

//     // println(pushedTransaction)

//     // println(Json.toJson(signed).toString + "\n\n")
//     // complexRequest.post(push_data).map(res => println(res.body))
// }

// lockAllWallets()
// -- get the current state of blockchain
// val eosApi: EosApi = EosApiFactory.create("http://127.0.0.1:8888")
// val arg: SignArg = eosApi.getSignArg(120)

// println(eosApi.getObjectMapper().writeValueAsString(arg))
// val transferArg: TransferArg = new TransferArg(from, "user2", "1.0000 EOS", "Executed from Server API")
// val argsJson = Json.obj("name" -> "user1")
// val abiJsonToBin: AbiJsonToBin = clientNodeosAPI.abiJsonToBin("hello", "hi", "00000000807015d6")
// val act = buildActions("eosio.token", "action", authorization(from), abiJsonToBin.getBinargs)



<!-- Defualt Transfer Action -->

// ② get the latest block info
def getLatestBlock(): Block = {
  clientNodeosAPI.getBlock(clientNodeosAPI.getChainInfo().getHeadBlockId())
  // println("blockNum=" + block.getBlockNum())
  // val block: Block = 
}

// ③ create the authorization
private def authorization(acc: Seq[String]): List[TransactionAuthorization] = new ArrayList(acc.map(x => new TransactionAuthorization(x, "active")).asJavaCollection)

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
private def signTransaction(acc: String, data: AbiJsonToBin, block: Block): SignedPackedTransaction = 
  clientKeosdAPI.signTransaction(
    packedTransaction(acc, block, authorization(Seq(acc)), data), 
    Arrays.asList(publicKey), 
    clientNodeosAPI.getChainInfo().getChainId())
// println("println("signedPackedTransaction" + mapper.writeValueAsString(signedPackedTransaction))

// ⑧ push the signed transaction 
// return 1 for success and 0 for failed tx..
def pushSignedTx(signedTx: SignedPackedTransaction): PushedTransaction =
  EosApiFactory.create(nodeosApiBaseURL).pushTransaction(null, signedTx)