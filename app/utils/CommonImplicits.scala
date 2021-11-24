package utils

import java.time.Instant
import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.format.Formatter
import play.api.data.format.Formats._
import models.domain._
import models.domain.eosio._
import models.domain.wallet.support._

trait CommonImplicits {
	// Common
	implicit def dateTimeStringToUnix(s: String): Option[Instant] =
    try {
      Some(Instant.parse(s))
    } catch {
      case _: Throwable => None
    }

 	implicit object DoubleFormatter extends Formatter[Double] {
    override val format = Some(("format.data", Nil))
    override def bind(key: String, data: Map[String, String]) = parsing(_.toDouble, "error.data", Nil)(key, data)
    override def unbind(key: String, value: Double) = Map(key -> value.toString)
  }

	// Models domain
	implicit def implGame = Json.format[Game]
	implicit def implGenre = Json.format[Genre]
	implicit def implicitData = Json.format[Data]
	implicit def implicitReceipt = Json.format[Receipt]
	implicit def implicitAct = Json.format[Act]
	implicit def implicitPartial = Json.format[Partial]
	implicit def implicitActionTrace = Json.format[ActionTrace]

	// Models Custom Validations
	implicit val implicitTraceReads: Reads[Trace] = new Reads[Trace] {
		override def reads(js: JsValue): JsResult[Trace] = js match {
			case json: JsValue => {
				try {
					JsSuccess(Trace(
						(json \ "id").as[String],
						(json \ "status").as[String],
						(json \ "cpu_usage_us").as[Int],
						(json \ "net_usage_words").as[Int],
						(json \ "elapsed").as[Int],
						(json \ "net_usage").as[Int],
						(json \ "scheduled").as[Boolean],
						(json \ "action_traces").as[Seq[ActionTrace]],
						(json \ "account_ram_delta").asOpt[String],
						(json \ "except").asOpt[String],
						(json \ "error_code").asOpt[String],
						(json \ "failed_dtrx_trace").as[JsValue],
						(json \ "partial").asOpt[Partial]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitTraceWrites = new Writes[Trace] {
	  def writes(tx: Trace): JsValue = Json.obj(
		"id" -> tx.id,
		"status" -> tx.status,
		"cpu_usage_us" -> tx.cpuUsageUs,
		"net_usage_words" -> tx.netUsageWords,
		"elapsed" -> tx.elapsed,
		"net_usage" -> tx.netUsage,
		"scheduled" -> tx.scheduled,
		"action_traces" -> tx.actTraces,
		"account_ram_delta" -> tx.accRamDelta,
		"except" -> tx.except,
		"error_code" -> tx.errorCode,
		"failed_dtrx_trace" -> tx.failedDtrxTrace,
		"partial" -> tx.partial)
	}
	implicit val implicitEOSNetTxReads: Reads[EOSNetTransaction] = new Reads[EOSNetTransaction] {
		override def reads(js: JsValue): JsResult[EOSNetTransaction] = js match {
			case json: JsValue => {
				try {
					JsSuccess(EOSNetTransaction(
						(json \ "id").as[UUID],
						(json \ "trace_id").as[String],
						(json \ "block_num").as[Long],
						(json \ "block_timestamp").as[Long],
					 	(json \ "trace").as[Trace]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitEOSNetTxWrites = new Writes[EOSNetTransaction] {
	  def writes(tx: EOSNetTransaction): JsValue = Json.obj(
		"id" -> tx.id,
		"trace_id" -> tx.traceId,
		"block_num" -> tx.blockNum,
		"block_timestamp" -> Instant.ofEpochSecond(tx.blockTimestamp),
		"trace" -> tx.trace)
	}
	implicit val implGQCharacterCreated = Json.format[GQCharacterCreated]
	implicit val implGQBattleTime = Json.format[GQBattleTime]
	implicit val implVIPWSRequest = Json.format[VIPWSRequest]
	implicit val implGQGetNextBattle = Json.format[GQGetNextBattle]
	// implicit val implTHGameData = Json.format[THGameData]
	implicit val implEOSNotifyTransaction = Json.format[EOSNotifyTransaction]
	implicit val implicitInEventMessageReads: Reads[InEventMessage] = {
    Json.format[GQCharacterCreated].map(x => x: InEventMessage) or
    Json.format[GQBattleTime].map(x => x: InEventMessage) or
    Json.format[GQGetNextBattle].map(x => x: InEventMessage) or
    Json.format[VIPWSRequest].map(x => x: InEventMessage) or
    Json.format[EOSNotifyTransaction].map(x => x: InEventMessage)
  }

  implicit val implicitInEventMessageWrites = new Writes[InEventMessage] {
    def writes(event: InEventMessage): JsValue = {
      event match {
        case m: GQCharacterCreated => Json.toJson(m)
        case m: GQBattleTime => Json.toJson(m)
        case m: GQGetNextBattle => Json.toJson(m)
        case m: VIPWSRequest => Json.toJson(m)
        case m: EOSNotifyTransaction => Json.toJson(m)
        case _ => Json.obj("error" -> "wrong Json")
      }
    }
  }

  implicit val implSubscribe = Json.format[Subscribe]
	implicit val implConnectionAlive = Json.format[ConnectionAlive]
  implicit val implicitInEventReads: Reads[InEvent] = new Reads[InEvent] {
		override def reads(js: JsValue): JsResult[InEvent] = js match {
			case json: JsValue => {
				try {
					JsSuccess(InEvent((json \ "id").as[JsString], (json \ "input").as[InEventMessage]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitInEventWrites = new Writes[InEvent] {
	  def writes(tx: InEvent): JsValue = {
	  	Json.toJson(tx.input)
	  	// if (tx.id == JsNull)
	  	// 	Json.obj("input" -> Json.toJson(tx.input))
	  	// else
	  	// 	Json.obj("id" -> tx.id, "input" -> Json.toJson(tx.input))
	  }
	}
  implicit val implicitOutEventReads: Reads[OutEvent] = new Reads[OutEvent] {
		override def reads(js: JsValue): JsResult[OutEvent] = js match {
			case json: JsValue => {
				try {
					JsSuccess(OutEvent((json \ "id").as[JsString], (json \ "response").as[JsValue]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitOutEventWrites = new Writes[OutEvent] {
	  def writes(tx: OutEvent): JsValue = {
	  	if (tx.id == JsNull)
	  		Json.obj("response" -> tx.response)
	  	else
	  		Json.obj("id" -> tx.id, "response" -> tx.response)
	  }
	}

  implicit def implGhostQuestCharactersRankByEarned = Json.format[GhostQuestCharactersRankByEarned]
  implicit def implGhostQuestCharactersRankByWinStreak = Json.format[GhostQuestCharactersRankByWinStreak]
  implicit def implGhostQuestCharactersLifeTimeWinStreak = Json.format[GhostQuestCharactersLifeTimeWinStreak]

  implicit def implGameType = Json.format[GameType]
	implicit def implPaymentType = Json.format[PaymentType]
	implicit def implBooleanPredictions = Json.format[BooleanPredictions]
	implicit def implListOfIntPredictions = Json.format[ListOfIntPredictions]
	implicit def implIntPredictions = Json.format[IntPredictions]
	implicit val implicitTransactionTypeReads: Reads[TransactionType] = {
	  Json.format[GameType].map(x => x: TransactionType) or
	  Json.format[PaymentType].map(x => x: TransactionType) or
	  Json.format[BooleanPredictions].map(x => x: TransactionType) or
	  Json.format[ListOfIntPredictions].map(x => x: TransactionType) or
	  Json.format[IntPredictions].map(x => x: TransactionType)
	}

	implicit val implicitTransactionTypeWrites = new Writes[TransactionType] {
	  def writes(event: TransactionType): JsValue = {
	    event match {
	      case m: GameType => Json.toJson(m)
	      case m: PaymentType => Json.toJson(m)
	      case m: BooleanPredictions => Json.toJson(m)
	      case m: ListOfIntPredictions => Json.toJson(m)
	      case m: IntPredictions => Json.toJson(m)
	      case _ => Json.obj("error" -> "wrong Json")
	    }
	  }
	}

	implicit val implicitOverAllGameHistoryReads: Reads[OverAllGameHistory] = new Reads[OverAllGameHistory] {
		override def reads(js: JsValue): JsResult[OverAllGameHistory] = js match {
			case json: JsValue => {
				try {
					JsSuccess(OverAllGameHistory(
						(json \ "id").as[UUID],
						(json \ "tx_hash").as[String],
						(json \ "game_id").as[String],
						(json \ "game").as[String],
						(json \ "info").as[TransactionType],
						(json \ "is_confirmed").as[Boolean],
						(json \ "created_at").as[Long]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitOverAllGameHistoryWrites = new Writes[OverAllGameHistory] {
	  def writes(tx: OverAllGameHistory): JsValue = Json.obj(
		"id" -> tx.id,
		"tx_hash" -> tx.txHash,
		"game_id" -> tx.gameID,
		"game" -> tx.game,
		"info" -> tx.info,
		"is_confirmed" -> tx.isConfirmed,
		"created_at" -> tx.createdAt)
	}

	implicit def implChallenge = Json.format[Challenge]
	implicit def implChallengeTracker = Json.format[ChallengeTracker]
	implicit def implChallengeHistory = Json.format[ChallengeHistory]
	implicit def implTaskHistory = Json.format[TaskHistory]
	implicit def implTaskGameInfo = Json.format[TaskGameInfo]
	implicit def implTask = Json.format[Task]
	implicit def implDailyTask = Json.format[DailyTask]

	implicit def implRankProfit = Json.format[RankProfit]
	implicit def implRankPayout = Json.format[RankPayout]
	implicit def implRankWagered = Json.format[RankWagered]
	implicit def implRankMultiplier = Json.format[RankMultiplier]
	implicit val implicitRankTypeReads: Reads[RankType] = {
	  Json.format[RankProfit].map(x => x: RankType) or
	  Json.format[RankPayout].map(x => x: RankType) or
	  Json.format[RankWagered].map(x => x: RankType) or
	  Json.format[RankMultiplier].map(x => x: RankType)
	}

	implicit val implicitRankTypeWrites = new Writes[RankType] {
	  def writes(event: RankType): JsValue = {
	    event match {
	      case m: RankProfit => Json.toJson(m)
	      case m: RankPayout => Json.toJson(m)
	      case m: RankWagered => Json.toJson(m)
	      case m: RankMultiplier => Json.toJson(m)
	      case _ => Json.obj("error" -> "wrong Json")
	    }
	  }
	}
	implicit def implRankingHistory = Json.format[RankingHistory]

	// https://gist.github.com/mikesname/5237809
	implicit def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }
  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }
  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = Format(enumReads(enum), enumWrites)
  import models.domain.enum._
  implicit def enumVIPFormat = enumFormat(VIP)
  implicit def enumVIPBenefitAmountFormat = enumFormat(VIPBenefitAmount)
  implicit def enumVIPBenefitPointsFormat = enumFormat(VIPBenefitPoints)
	implicit val implicitVIPUserReads: Reads[VIPUser] = new Reads[VIPUser] {
		override def reads(js: JsValue): JsResult[VIPUser] = js match {
			case json: JsValue => {
				try {
					JsSuccess(VIPUser(
						(json \ "id").as[UUID],
						(json \ "rank").as[VIP.Value],
						(json \ "next_rank").as[VIP.Value],
						(json \ "referral_count").as[Int],
						(json \ "payout").as[Double],
						(json \ "points").as[Double],
						(json \ "updated_at").as[Instant]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	// 0 - 100 = Bronze
	// 101 - 500 = Silver
	// 501 and 1000up  = Gold
	implicit val implicitVIPUserWrites = new Writes[VIPUser] {
	  def writes(tx: VIPUser): JsValue = Json.obj(
		"id" -> tx.id,
		"rank" -> tx.rank,
		"next_rank" -> tx.next_rank,
		"referral_count" -> tx.referral_count,
		"payout" -> tx.payout,
		"points" -> tx.points,
		"percentage" -> {
			val hasPrevLvl: Double = tx.points - tx.prevLvlMax()
			val dividend: Double = hasPrevLvl / tx.currentLvlMax()
			(dividend * 100)
		 },
		"updated_at" -> tx.updated_at)
	}
	implicit def implVIPBenefit = Json.format[VIPBenefit]

	implicit val implicitUserAccountReads: Reads[UserAccount] = new Reads[UserAccount] {
		override def reads(js: JsValue): JsResult[UserAccount] = js match {
			case json: JsValue => {
				try {
					JsSuccess(UserAccount(
						(json \ "id").as[UUID],
						(json \ "user_game_id").as[Int],
						(json \ "username").as[String],
						(json \ "password").as[String],
						(json \ "email").asOpt[String],
						(json \ "referred_by").asOpt[String],
						(json \ "referral_code").as[String],
						(json \ "referral").as[Double],
						(json \ "referral_rate").as[Double],
						(json \ "win_rate").as[Double],
						(json \ "is_verified").as[Boolean],
						(json \ "last_sign_in").as[Instant],
						(json \ "created_at").as[Instant]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitUserAccountWrites = new Writes[UserAccount] {
	  def writes(tx: UserAccount): JsValue = Json.obj(
		"id" -> tx.id,
		"user_game_id" -> tx.userGameID,
		"username" -> tx.username,
		// "password" -> tx.password,
		"email" -> tx.email,
		"referred_by" -> tx.referredBy,
		"referral_code" -> tx.referralCode,
		"referral" -> tx.referral,
		"referral_rate" -> tx.referralRate,
		"win_rate" -> tx.winRate,
		"is_verified" -> tx.isVerified,
		"last_sign_in" -> tx.lastSignIn,
		"created_at" -> tx.createdAt)
	}
	implicit def implClientTokenEndpoint = Json.format[ClientTokenEndpoint]
	implicit def implWalletSupportCoin = Json.format[Coin]
	implicit val implicitUserAccountWalletReads: Reads[UserAccountWallet] = new Reads[UserAccountWallet] {
		override def reads(js: JsValue): JsResult[UserAccountWallet] = js match {
			case json: JsValue => {
				try {
					JsSuccess(UserAccountWallet(
						(json \ "id").as[UUID],
						(json \ "btc").as[Coin],
						(json \ "eth").as[Coin],
						(json \ "usdc").as[Coin]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitUserAccountWalletWrites = new Writes[UserAccountWallet] {
	  def writes(tx: UserAccountWallet): JsValue = Json.obj(
			"id" -> tx.id,
			"btc" -> tx.btc,
			"eth" -> tx.eth,
			"usdc" -> tx.usdc)
	}
	implicit val implWalletSupportCoinDepositReads: Reads[CoinDeposit] = new Reads[CoinDeposit] {
		override def reads(js: JsValue): JsResult[CoinDeposit] = js match {
			case json: JsValue => {
				try {
					JsSuccess(CoinDeposit(
						(json \ "tx_hash").as[String],
						(json \ "issuer").as[Coin],
						(json \ "receiver").as[Coin]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implWalletSupportCoinDepositWrites = new Writes[CoinDeposit] {
	  def writes(tx: CoinDeposit): JsValue = Json.obj(
			"tx_hash" -> tx.txHash,
			"issuer" -> tx.issuer,
			"receiver" -> tx.receiver)
	}
	implicit val implWalletSupportCoinWithdrawReads: Reads[CoinWithdraw] = new Reads[CoinWithdraw] {
		override def reads(js: JsValue): JsResult[CoinWithdraw] = js match {
			case json: JsValue => {
				try {
					JsSuccess(CoinWithdraw((json \ "receiver").as[Coin], (json \ "gasPrice").as[BigDecimal]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implWalletSupportCoinWithdrawWrites = new Writes[CoinWithdraw] {
	  def writes(tx: CoinWithdraw): JsValue = Json.obj("receiver" -> tx.receiver, "gasPrice" -> tx.gasPrice)
	}
	implicit def implicitFailedCoinDeposit = Json.format[FailedCoinDeposit]
	implicit val implETHJsonRpcResultReads: Reads[ETHJsonRpcResult] = new Reads[ETHJsonRpcResult] {
		override def reads(js: JsValue): JsResult[ETHJsonRpcResult] = js match {
			case json: JsValue => {
				try {
					JsSuccess(ETHJsonRpcResult(
						(json \ "accessList").asOpt[JsValue],
						(json \ "blockHash").as[String],
						(json \ "blockNumber").as[Long],
						(json \ "chainId").asOpt[String],
						(json \ "condition").asOpt[String],
						(json \ "creates").asOpt[String],
						(json \ "from").as[String],
						(json \ "gas").as[Long],
						(json \ "gasPrice").as[Double],
						(json \ "hash").as[String],
						(json \ "input").as[String],
						(json \ "maxFeePerGas").asOpt[String],
						(json \ "maxPriorityFeePerGas").asOpt[String],
						(json \ "nonce").as[Int],
						(json \ "publicKey").asOpt[String],
						(json \ "raw").asOpt[String],
						(json \ "standardV").asOpt[String],
						(json \ "to").as[String],
						(json \ "transactionIndex").as[String],
						(json \ "value").as[String],
						(json \ "type").asOpt[String],
						(json \ "v").as[String],
						(json \ "r").as[String],
						(json \ "s").as[String]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implETHJsonRpcResultWrites = new Writes[ETHJsonRpcResult] {
	  def writes(tx: ETHJsonRpcResult): JsValue = Json.obj(
			"accessList" -> tx.accessList,
			"blockHash" -> tx.blockHash,
			"blockNumber" -> tx.blockNumber,
			"chainId" -> tx.chainId,
			"condition" -> tx.condition,
			"creates" -> tx.creates,
			"from" -> tx.from,
			"gas" -> tx.gas,
			"gasPrice" -> tx.gasPrice,
			"hash" -> tx.hash,
			"input" -> tx.input,
			"maxFeePerGas" -> tx.maxFeePerGas,
			"maxPriorityFeePerGas" -> tx.maxPriorityFeePerGas,
			"nonce" -> tx.nonce,
			"publicKey" -> tx.publicKey,
			"raw" -> tx.raw,
			"standardV" -> tx.standardV,
			"to" -> tx.to,
			"transactionIndex" -> tx.transactionIndex,
			"value" -> tx.value,
			"type" -> tx.`type`,
			"v" -> tx.v,
			"r" -> tx.r,
			"s" -> tx.s)
	}
	implicit val implETHJsonRpc = Json.format[ETHJsonRpc]
	implicit val implicitCryptoJsonRpcReads: Reads[CryptoJsonRpc] = {
	  Json.format[ETHJsonRpc].map(x => x: CryptoJsonRpc)
	}
	implicit val implicitCryptoJsonRpcWrites = new Writes[CryptoJsonRpc] {
	  def writes(param: CryptoJsonRpc): JsValue = {
	    param match {
	      case v: ETHJsonRpc => Json.toJson(v)
	      case _ => Json.obj("error" -> "wrong Json")
	    }
	  }
	}
	implicit val implicitCryptoJsonRpcHistoryReads: Reads[CryptoJsonRpcHistory] = {
		implETHJsonRpcResultReads.map(x => x: CryptoJsonRpcHistory)
	}
	implicit val implicitCryptoJsonRpcHistoryWrites = new Writes[CryptoJsonRpcHistory] {
	  def writes(param: CryptoJsonRpcHistory): JsValue = {
	    param match {
	      case v: ETHJsonRpcResult => Json.toJson(v)
	      case _ => Json.obj("error" -> "wrong Json")
	    }
	  }
	}
	implicit val implUserAccountWalletHistory = Json.format[UserAccountWalletHistory]
	implicit val implETHUSDCWithdrawEvent = Json.format[ETHUSDCWithdrawEvent]
	implicit val implDepositEvent = Json.format[DepositEvent]
	implicit val implicitEventReads: Reads[Event] = {
		Json.format[InEvent].map(x => x: Event) or
	  Json.format[OutEvent].map(x => x: Event) or
	  Json.format[Subscribe].map(x => x: Event) or
	  Json.format[ConnectionAlive].map(x => x: Event) or
	  Json.format[DepositEvent].map(x => x: Event) or
	  Json.format[ETHUSDCWithdrawEvent].map(x => x: Event)
  }

  implicit val implicitEventWrites = new Writes[Event] {
    def writes(event: Event): JsValue = {
      event match {
        case m: InEvent => Json.toJson(m)
        case m: OutEvent => Json.toJson(m)
        case m: Subscribe => Json.toJson(m)
        case m: ConnectionAlive => Json.toJson(m)
        case m: DepositEvent => Json.toJson(m)
        case m: ETHUSDCWithdrawEvent => Json.toJson(m)
        case _ => Json.obj("error" -> "wrong Json")
      }
    }
  }

  implicit val implCoinCapAssetReads: Reads[CoinCapAsset] = new Reads[CoinCapAsset] {
		override def reads(js: JsValue): JsResult[CoinCapAsset] = js match {
			case json: JsValue => {
				try {
					JsSuccess(CoinCapAsset(
						(json \ "id").as[String],
						try { (json \ "rank").as[String].toInt } catch { case _: Throwable => 0 },
						(json \ "symbol").as[String],
						(json \ "name").as[String],
						try { BigDecimal((json \ "supply").as[String]) } catch { case _: Throwable => 0 },
						try { BigDecimal((json \ "maxSupply").as[String]) } catch { case _: Throwable => 0 },
						try { BigDecimal((json \ "marketCapUsd").as[String]) } catch { case _: Throwable => 0 },
						(json \ "volumeUsd24Hr").as[String],
						try { BigDecimal((json \ "priceUsd").as[String]) } catch { case _: Throwable => 0 },
						(json \ "changePercent24Hr").as[String],
						(json \ "vwap24Hr").as[String]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implCoinCapAssetWrites = new Writes[CoinCapAsset] {
	  def writes(v: CoinCapAsset): JsValue = Json.obj(
			"id" -> v.id,
			"rank" -> v.rank,
			"symbol" -> v.symbol,
			"name" -> v.name,
			"supply" -> v.supply,
			"maxSupply" -> v.maxSupply,
			"marketCapUsd" -> v.marketCapUsd,
			"volumeUsd24Hr" -> v.volumeUsd24Hr,
			"priceUsd" -> v.priceUsd,
			"changePercent24Hr" -> v.changePercent24Hr,
			"vwap24Hr" -> v.vwap24Hr)
	}

	implicit def implicitTreasureHuntGameDataPanelSet = Json.format[TreasureHuntGameDataPanelSet]
	implicit def implicitTreasureHuntGameData = Json.format[TreasureHuntGameData]
	implicit def implicitMahjongHiloScore = Json.format[MahjongHiloScore]
	implicit def implicitMahjongHiloTile = Json.format[MahjongHiloTile]
	implicit def implicitMahjongHiloWinnables = Json.format[MahjongHiloWinnables]
	implicit val implMahjongHiloGameDataReads: Reads[MahjongHiloGameData] = new Reads[MahjongHiloGameData] {
		override def reads(js: JsValue): JsResult[MahjongHiloGameData] = js match {
			case json: JsValue => {
				try {
					JsSuccess(MahjongHiloGameData(
						(json \ "game_id").as[String],
						(json \ "deck_player").as[Seq[Int]],
						(json \ "status").as[Int],
						try { BigDecimal((json \ "hi_lo_balance").as[String]) } catch { case _: Throwable => 0 },
						(json \ "prediction").as[Int],
						(json \ "hi_lo_result").as[Int],
						(json \ "hi_lo_outcome").as[Int],
						try { BigDecimal((json \ "hi_lo_bet").as[String]) } catch { case _: Throwable => 0 },
						try { BigDecimal((json \ "hi_lo_stake").as[String]) } catch { case _: Throwable => 0 },
						try { BigDecimal((json \ "low_odds").as[String]) } catch { case _: Throwable => 0 },
						try { BigDecimal((json \ "draw_odds").as[String]) } catch { case _: Throwable => 0 },
						try { BigDecimal((json \ "high_odds").as[String]) } catch { case _: Throwable => 0 },
						(json \ "bet_status").as[Int],
						(json \ "option_status").as[Int],
						(json \ "riichi_status").as[Int],
						(json \ "sumofvalue").as[Seq[Int]],
						(json \ "prevalent_wind").as[Int],
						(json \ "seat_wind").as[Int],
						(json \ "current_tile").as[Int],
						(json \ "standard_tile").as[Int],
						(json \ "eye_idx").as[Int],
						(json \ "pair_count").as[Int],
						(json \ "pung_count").as[Int],
						(json \ "chow_count").as[Int],
						(json \ "kong_count").as[Int],
						(json \ "draw_count").as[Int],
						(json \ "hand_player").as[Seq[Int]],
						(json \ "discarded_tiles").as[Seq[Int]],
						(json \ "reveal_kong").as[Seq[Int]],
						(json \ "winning_hand").as[Seq[Int]],
						(json \ "score_check").as[Seq[Int]],
						(json \ "score_type").as[Seq[MahjongHiloScore]],
						(json \ "wintiles").as[Seq[MahjongHiloWinnables]],
						(json \ "final_score").as[Int]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implMahjongHiloGameDataWrites = new Writes[MahjongHiloGameData] {
	  def writes(v: MahjongHiloGameData): JsValue = Json.obj(
			"game_id" -> v.game_id,
			"deck_player" -> v.deck_player,
			"status" -> v.status,
			"hi_lo_balance" -> v.hi_lo_balance,
			"prediction" -> v.prediction,
			"hi_lo_result" -> v.hi_lo_result,
			"hi_lo_outcome" -> v.hi_lo_outcome,
			"hi_lo_bet" -> v.hi_lo_bet,
			"hi_lo_stake" -> v.hi_lo_stake,
			"low_odds" -> v.low_odds,
			"draw_odds" -> v.draw_odds,
			"high_odds" -> v.high_odds,
			"bet_status" -> v.bet_status,
			"option_status" -> v.option_status,
			"riichi_status" -> v.riichi_status,
			"sumofvalue" -> v.sumofvalue,
			"prevalent_wind" -> v.prevalent_wind,
			"seat_wind" -> v.seat_wind,
			"current_tile" -> v.current_tile,
			"standard_tile" -> v.standard_tile,
			"eye_idx" -> v.eye_idx,
			"pair_count" -> v.pair_count,
			"pung_count" -> v.pung_count,
			"chow_count" -> v.chow_count,
			"kong_count" -> v.kong_count,
			"draw_count" -> v.draw_count,
			"hand_player" -> v.hand_player,
			"discarded_tiles" -> v.discarded_tiles,
			"reveal_kong" -> v.reveal_kong,
			"winning_hand" -> v.winning_hand,
			"score_check" -> v.score_check,
			"score_type" -> v.score_type,
			"wintiles" -> v.wintiles,
			"final_score" -> v.final_score)
	}
	implicit val implGhostQuestCharacterValueReads: Reads[GhostQuestCharacterValue] = new Reads[GhostQuestCharacterValue] {
		override def reads(js: JsValue): JsResult[GhostQuestCharacterValue] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GhostQuestCharacterValue(
						(json \ "owner_id").as[Int],
						(json \ "ghost_name").as[String],
						(json \ "ghost_id").as[Int],
						(json \ "rarity").as[Int],
						(json \ "character_life").as[Int],
						(json \ "hitpoints").as[Int],
						(json \ "status").as[Int],
						(json \ "attack").as[Int],
						(json \ "defense").as[Int],
						(json \ "speed").as[Int],
						(json \ "luck").as[Int],
						try { BigDecimal((json \ "prize").as[String]) } catch { case _: Throwable => 0 },
						(json \ "battle_limit").as[Int],
						(json \ "battle_count").as[Int],
						(json \ "created_at").as[String],
						(json \ "last_match").as[Int],
						(json \ "enemy_fought").as[JsValue]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implGhostQuestCharacterValueWrites = new Writes[GhostQuestCharacterValue] {
	  def writes(v: GhostQuestCharacterValue): JsValue = Json.obj(
			"owner_id" -> v.owner_id,
			"ghost_name" -> v.ghost_name,
			"ghost_id" -> v.ghost_id,
			"rarity" -> v.rarity,
			"character_life" -> v.character_life,
			"hitpoints" -> v.hitpoints,
			"status" -> v.status,
			"attack" -> v.attack,
			"defense" -> v.defense,
			"speed" -> v.speed,
			"luck" -> v.luck,
			"prize" -> v.prize,
			"battle_limit" -> v.battle_limit,
			"battle_count" -> v.battle_count,
			"created_at" -> v.created_at,
			"last_match" -> v.last_match,
			"enemy_fought" -> v.enemy_fought)
	}
	implicit val implMahjongHiloHistoryReads: Reads[MahjongHiloHistory] = new Reads[MahjongHiloHistory] {
		override def reads(js: JsValue): JsResult[MahjongHiloHistory] = js match {
			case json: JsValue => {
				try {
					JsSuccess(MahjongHiloHistory(
						(json \ "game_id").as[String],
						(json \ "user_game_id").as[Int],
						(json \ "predictions").as[Seq[(Int, Int, Int, Int)]],
						(json \ "game_data").asOpt[MahjongHiloGameData],
						(json \ "status").as[Boolean],
						(json \ "created_at").as[Instant]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implMahjongHiloHistoryWrites = new Writes[MahjongHiloHistory] {
	  def writes(v: MahjongHiloHistory): JsValue = Json.obj(
			"game_id" -> v.gameID,
			"user_game_id" -> v.userGameID,
			"predictions" -> v.predictions,
			"game_data" -> v.gameData,
			"status" -> v.status,
			"created_at" -> v.createdAt)
	}
	implicit def implicitGhostQuestCharacter = Json.format[GhostQuestCharacter]
	implicit def implicitGhostQuestGameData = Json.format[GhostQuestGameData]
	implicit def implicitGhostQuestTableGameData = Json.format[GhostQuestTableGameData]
	implicit def implicitGhostQuestCharacterHistory = Json.format[GhostQuestCharacterHistory]
	implicit def implicitGhostQuestCharacterGameLog = Json.format[GhostQuestCharacterGameLog]
	implicit def implicitGhostQuestCharacterGameHistory = Json.format[GhostQuestCharacterGameHistory]
	implicit def implicitGhostQuestBattleResult = Json.format[GhostQuestBattleResult]
}