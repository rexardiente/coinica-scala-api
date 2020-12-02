
#### import classes
		play.api.libs.ws.WSClient // inject to Controller
		models.domain.eosio.TableRowsRequest
		utils.lib.EOSIOSupport

#### Get All GQ Active Users
		val req = new TableRowsRequest("ghostquest", "users", "ghostquest", None, Some("uint64_t"), None, None, None)
    new utils.lib.EOSIOSupport(ws).getGQUsers(req).map(Ok(_))


#### Sample testing Hello Contract
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

> Note: Make TableRowsRequest as dynamic class object that can handle all EOSIO related table requests.