package utils

import java.time.Instant
import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.format.Formatter
import play.api.data.format.Formats._
import models.domain._
import models.domain.eosio._
import models.domain.eosio.GQ.v2._
import models.domain.multi.currency._

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
implicit val implicitGQCharacterInfoReads: Reads[GQCharacterInfo] = new Reads[GQCharacterInfo] {
		override def reads(js: JsValue): JsResult[GQCharacterInfo] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterInfo(
						(json \ "LIFE").as[Int],
						(json \ "HP").as[Int],
						(json \ "CLASS").as[Int],
						(json \ "LEVEL").as[Int],
						(json \ "STATUS").as[Int],
						(json \ "ATTACK").as[Int],
						(json \ "DEFENSE").as[Int],
						(json \ "SPEED").as[Int],
						(json \ "LUCK").as[Int],
						(json \ "GAME_LIMIT").as[Int],
						(json \ "GAME_COUNT").as[Int],
						(json \ "CREATED_AT").asOpt[String].map(_.slice(0, 10).toLong).getOrElse(0)
					))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitGQCharacterInfoWrites = new Writes[GQCharacterInfo] {
	  def writes(tx: GQCharacterInfo): JsValue = Json.obj(
			"LIFE" -> tx.life,
			"HP" -> tx.hp,
			"CLASS" -> tx.`class`,
			"LEVEL" -> tx.level,
			"STATUS" -> tx.status,
			"ATTACK" -> tx.attack,
			"DEFENSE" -> tx.defense,
			"SPEED" -> tx.speed,
			"LUCK" -> tx.luck,
			"GAME_LIMIT" -> tx.limit,
			"GAME_COUNT" -> tx.count,
			"CREATED_AT" -> tx.createdAt)
	}
	// implicit def implGQGhost = Json.format[GQGhost]
	implicit val implicitGQGhostReads: Reads[GQGhost] = new Reads[GQGhost] {
		override def reads(js: JsValue): JsResult[GQGhost] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQGhost(
						(json \ "key").as[String],
						(json \ "value").as[GQCharacterInfo]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitGQGhostWrites = new Writes[GQGhost] {
	  def writes(tx: GQGhost): JsValue = Json.obj("key" -> tx.key, "value" -> tx.value)
	}

	implicit def implGQGame = Json.format[GQGame]
	implicit def implGQTable = Json.format[GQTable]
	implicit def implGQRowsResponse = Json.format[GQRowsResponse]
	implicit def implGQGameStatus = Json.format[GQGameStatus]
	implicit val implicitGQCharacterGameHistoryReads: Reads[GQCharacterGameHistory] = new Reads[GQCharacterGameHistory] {
		override def reads(js: JsValue): JsResult[GQCharacterGameHistory] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterGameHistory(
						(json \ "game_id").as[String],
						(json \ "tx_hash").as[String],
						(json \ "winner").as[UUID],
						(json \ "winner_id").as[String],
						(json \ "loser").as[UUID],
						(json \ "loser_id").as[String],
						(json \ "gameplay_log").as[List[GameLog]],
						(json \ "time_executed").as[Long]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitGQCharacterGameHistoryWrites = new Writes[GQCharacterGameHistory] {
	  def writes(tx: GQCharacterGameHistory): JsValue = Json.obj(
		"game_id" -> tx.id,
		"tx_hash" -> tx.txHash,
		"winner" -> tx.winner,
		"winner_id" -> tx.winnerID,
		"loser" -> tx.loser,
		"loser_id" -> tx.loserID,
		"gameplay_log" -> tx.logs,
		"time_executed" -> tx.timeExecuted)
	}

	implicit val implicitGQCharacterDataReads: Reads[GQCharacterData] = new Reads[GQCharacterData] {
    override def reads(js: JsValue): JsResult[GQCharacterData] = js match {
      case json: JsValue => {
        try {
          JsSuccess(GQCharacterData(
            (json \ "KEY").as[String],
            (json \ "OWNER").as[UUID],
            (json \ "LIFE").as[Int],
            (json \ "HP").as[Int],
            (json \ "CLASS").as[Int],
            (json \ "LEVEL").as[Int],
            (json \ "STATUS").as[Int],
            (json \ "ATTACK").as[Int],
            (json \ "DEFENSE").as[Int],
            (json \ "SPEED").as[Int],
            (json \ "LUCK").as[Int],
            (json \ "GAME_LIMIT").as[Int],
            (json \ "GAME_COUNT").as[Int],
            (json \ "IS_NEW").as[Boolean],
            (json \ "CREATED_AT").as[Long]))
        } catch {
          case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
        }
      }
      case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
    }
  }
  implicit val implicitGQCharacterDataWrites = new Writes[GQCharacterData] {
    def writes(tx: GQCharacterData): JsValue = Json.obj(
    "KEY" -> tx.key,
    "OWNER" -> tx.owner,
    "LIFE" -> tx.life,
    "HP" -> tx.hp,
    "CLASS" -> tx.`class`,
    "LEVEL" -> tx.level,
    "STATUS" -> tx.status,
    "ATTACK" -> tx.attack,
    "DEFENSE" -> tx.defense,
    "SPEED" -> tx.speed,
    "LUCK" -> tx.luck,
    "GAME_LIMIT" -> tx.limit,
    "GAME_COUNT" -> tx.count,
    "IS_NEW" -> tx.isNew,
    "CREATED_AT" -> tx.createdAt)
  }

  implicit val implicitGQCharacterDataHistoryReads: Reads[GQCharacterDataHistory] = new Reads[GQCharacterDataHistory] {
    override def reads(js: JsValue): JsResult[GQCharacterDataHistory] = js match {
      case json: JsValue => {
        try {
          JsSuccess(GQCharacterDataHistory(
            (json \ "KEY").as[String],
            (json \ "OWNER").as[UUID],
            (json \ "LIFE").as[Int],
            (json \ "HP").as[Int],
            (json \ "CLASS").as[Int],
            (json \ "LEVEL").as[Int],
            (json \ "STATUS").as[Int],
            (json \ "ATTACK").as[Int],
            (json \ "DEFENSE").as[Int],
            (json \ "SPEED").as[Int],
            (json \ "LUCK").as[Int],
            (json \ "GAME_LIMIT").as[Int],
            (json \ "GAME_COUNT").as[Int],
            (json \ "IS_NEW").as[Boolean],
            (json \ "CREATED_AT").as[Long]))
        } catch {
          case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
        }
      }
      case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
    }
  }

  implicit val implicitGQCharacterDataHistoryWrites = new Writes[GQCharacterDataHistory] {
    def writes(tx: GQCharacterDataHistory): JsValue = Json.obj(
    "KEY" -> tx.key,
    "OWNER" -> tx.owner,
    "LIFE" -> tx.life,
    "HP" -> tx.hp,
    "CLASS" -> tx.`class`,
    "LEVEL" -> tx.level,
    "STATUS" -> tx.status,
    "ATTACK" -> tx.attack,
    "DEFENSE" -> tx.defense,
    "SPEED" -> tx.speed,
    "LUCK" -> tx.luck,
    "GAME_LIMIT" -> tx.limit,
    "GAME_COUNT" -> tx.count,
    "IS_NEW" -> tx.isNew,
    "CREATED_AT" -> tx.createdAt)
  }

	implicit val implicitGQCharacterDataHistoryLogsReads: Reads[GQCharacterDataHistoryLogs] = new Reads[GQCharacterDataHistoryLogs] {
		override def reads(js: JsValue): JsResult[GQCharacterDataHistoryLogs] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterDataHistoryLogs(
						(json \ "game_id").as[String],
						(json \ "is_win").as[List[GQGameStatus]],
						(json \ "logs").as[List[GameLog]],
						(json \ "time_executed").as[Long]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitGQCharacterDataHistoryLogsWrites = new Writes[GQCharacterDataHistoryLogs] {
	  def writes(tx: GQCharacterDataHistoryLogs): JsValue = Json.obj(
		"game_id" -> tx.gameID,
		"is_win" -> tx.isWin,
		"logs" -> tx.logs,
		"time_executed" -> tx.timeExecuted)
	}

	// implicit val implicitGQCharacterCreatedReads: Reads[GQCharacterCreated] = new Reads[GQCharacterCreated] {
	// 	override def reads(js: JsValue): JsResult[GQCharacterCreated] = js match {
	// 		case json: JsValue => {
	// 			try {
	// 				JsSuccess(GQCharacterCreated((json \ "character_created").as[Boolean]))
	// 			} catch {
	// 				case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
	// 			}
	// 		}
	// 		case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
	// 	}
	// }
	// implicit val implicitGQCharacterCreatedWrites = new Writes[GQCharacterCreated] {
	//   def writes(tx: GQCharacterCreated): JsValue = Json.obj("character_created" -> tx.characterCreated)
	// }
	// implicit val implicitGQBattleTimeReads: Reads[GQBattleTime] = new Reads[GQBattleTime] {
	// 	override def reads(js: JsValue): JsResult[GQBattleTime] = js match {
	// 		case json: JsValue => {
	// 			try {
	// 				JsSuccess(GQBattleTime((json \ "battle_time").as[String]))
	// 			} catch {
	// 				case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
	// 			}
	// 		}
	// 		case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
	// 	}
	// }
	// implicit val implicitGQBattleTimeWrites = new Writes[GQBattleTime] {
	//   def writes(tx: GQBattleTime): JsValue = Json.obj("battle_time" -> tx.battleTime)
	// }

	implicit val implGQCharacterCreated = Json.format[GQCharacterCreated]
	implicit val implGQBattleTime = Json.format[GQBattleTime]
	implicit val implVIPWSRequest = Json.format[VIPWSRequest]
	implicit val implGQGetNextBattle = Json.format[GQGetNextBattle]
	implicit val implTHPanelSet = Json.format[THPanelSet]

	implicit val implicitTHGameDataReads: Reads[THGameData] = new Reads[THGameData] {
		override def reads(js: JsValue): JsResult[THGameData] = js match {
			case json: JsValue => {
				try {
					JsSuccess(THGameData(
						(json \ "destination").as[Int],
						(json \ "enemy_count").as[Int],
						try {
							(json \ "maxprize").as[String].reverse.splitAt(4)._2.reverse.toDouble
						} catch {
							case _: Throwable => 0D
						},
						try {
							(json \ "nextprize").as[String].reverse.splitAt(4)._2.reverse.toDouble
						} catch {
							case _: Throwable => 0D
						},
						try {
							(json \ "odds").as[String].toDouble
						} catch {
							case _: Throwable => 0D
						},
						(json \ "panel_set").as[Seq[THPanelSet]],
						try {
							(json \ "prize").as[String].reverse.splitAt(4)._2.reverse.toDouble
						} catch {
							case _: Throwable => 0D
						},
						(json \ "status").as[Int],
						(json \ "unopentile").as[Int],
						(json \ "win_count").as[Int]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitTHGameDataWrites = new Writes[THGameData] {
	  def writes(tx: THGameData): JsValue = Json.obj(
		"destination" -> tx.destination,
		"enemy_count" -> tx.enemy_count,
		"maxprize" -> tx.maxprize,
		"nextprize" -> tx.nextprize,
		"odds" -> tx.odds,
		"panel_set" -> tx.panel_set,
		"prize" -> tx.prize,
		"status" -> tx.status,
		"unopentile" -> tx.unopentile,
		"win_count" -> tx.win_count)
	}

	// implicit val implTHGameData = Json.format[THGameData]
	implicit val implTHGameResult = Json.format[THGameResult]
	implicit val implEOSNotifyTransaction = Json.format[EOSNotifyTransaction]

	implicit val implicitInEventMessageReads: Reads[InEventMessage] = {
    Json.format[GQCharacterCreated].map(x => x: InEventMessage) or
    Json.format[GQBattleTime].map(x => x: InEventMessage) or
    Json.format[GQGetNextBattle].map(x => x: InEventMessage) or
    Json.format[VIPWSRequest].map(x => x: InEventMessage) or
    Json.format[THGameResult].map(x => x: InEventMessage) or
    Json.format[EOSNotifyTransaction].map(x => x: InEventMessage)
  }

  implicit val implicitInEventMessageWrites = new Writes[InEventMessage] {
    def writes(event: InEventMessage): JsValue = {
      event match {
        case m: GQCharacterCreated => Json.toJson(m)
        case m: GQBattleTime => Json.toJson(m)
        case m: GQGetNextBattle => Json.toJson(m)
        case m: VIPWSRequest => Json.toJson(m)
        case m: THGameResult => Json.toJson(m)
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
  implicit val implicitGQCharacterDataTraitReads: Reads[GQCharacterDataTrait] = {
		Json.format[GQCharacterData].map(x => x: GQCharacterDataTrait) or
    Json.format[GQCharacterDataHistory].map(x => x: GQCharacterDataTrait)
  }

  implicit val implicitGQCharacterDataTraitWrites = new Writes[GQCharacterDataTrait] {
    def writes(v: GQCharacterDataTrait): JsValue = v match {
      case m: GQCharacterData => Json.toJson(m)
      case m: GQCharacterDataHistory => Json.toJson(m)
      case _ => Json.obj("error" -> "wrong Json")
    }
  }

  implicit def implGQCharactersRankByEarned = Json.format[GQCharactersRankByEarned]
  implicit def implGQCharactersRankByWinStreak = Json.format[GQCharactersRankByWinStreak]
  implicit def implGQCharactersLifeTimeWinStreak = Json.format[GQCharactersLifeTimeWinStreak]

  implicit def implGameType = Json.format[GameType]
	implicit def implPaymentType = Json.format[PaymentType]
	implicit def implGQGameHistory = Json.format[GQGameHistory]
	implicit def implTHGameHistory = Json.format[THGameHistory]
	implicit val implicitTransactionTypeReads: Reads[TransactionType] = {
	  Json.format[GameType].map(x => x: TransactionType) or
	  Json.format[PaymentType].map(x => x: TransactionType) or
	  Json.format[GQGameHistory].map(x => x: TransactionType) or
	  Json.format[THGameHistory].map(x => x: TransactionType)
	}

	implicit val implicitTransactionTypeWrites = new Writes[TransactionType] {
	  def writes(event: TransactionType): JsValue = {
	    event match {
	      case m: GameType => Json.toJson(m)
	      case m: PaymentType => Json.toJson(m)
	      case m: GQGameHistory => Json.toJson(m)
	      case m: THGameHistory => Json.toJson(m)
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
		"tx_hash" -> tx.tx_hash,
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
	implicit def implVIPUser = Json.format[VIPUser]
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
	implicit def implUserToken = Json.format[UserToken]
	implicit def implClientTokenEndpoint = Json.format[ClientTokenEndpoint]
	// Multi Currency
	implicit def implCoin = Json.format[Coin]
	implicit def implLimits = Json.format[Limits]
	implicit def implWalletAddress = Json.format[WalletAddress]
	implicit def implGenerateOffer = Json.format[GenerateOffer]
	implicit def implCreateOrderTx = Json.format[CreateOrderTx]
	implicit def implCreateOrder = Json.format[CreateOrder]
	implicit def implCreateOrderResponse = Json.format[CreateOrderResponse]
	implicit def implOrderStatus = Json.format[OrderStatus]
	implicit def implListOfOrders = Json.format[ListOfOrders]
	implicit def implKeyPairGeneratorResponse = Json.format[KeyPairGeneratorResponse]
	implicit def implWalletKey = Json.format[WalletKey]
	// implicit def implUserAccountWallet = Json.format[UserAccountWallet]

	import models.domain.wallet.support._
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
					JsSuccess(CoinWithdraw((json \ "receiver").as[Coin], (json \ "fee").as[Long]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implWalletSupportCoinWithdrawWrites = new Writes[CoinWithdraw] {
	  def writes(tx: CoinWithdraw): JsValue = Json.obj("receiver" -> tx.receiver, "fee" -> tx.fee)
	}

	implicit val implETHJsonRpcResultReads: Reads[ETHJsonRpcResult] = new Reads[ETHJsonRpcResult] {
		override def reads(js: JsValue): JsResult[ETHJsonRpcResult] = js match {
			case json: JsValue => {
				try {
					JsSuccess(ETHJsonRpcResult(
						(json \ "blockHash").as[String],
						(json \ "blockNumber").as[Long],
						(json \ "chainId").asOpt[String],
						(json \ "condition").asOpt[String],
						(json \ "creates").asOpt[String],
						(json \ "from").as[String],
						(json \ "gas").as[Long],
						(json \ "gasPrice").as[Int],
						(json \ "hash").as[String],
						(json \ "input").as[String],
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
	  Json.format[ETHJsonRpcResult].map(x => x: CryptoJsonRpcHistory)
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
	implicit val implETHWalletTxEvent = Json.format[ETHWalletTxEvent]
	implicit val implicitEventReads: Reads[Event] = {
		Json.format[InEvent].map(x => x: Event) or
    Json.format[OutEvent].map(x => x: Event) or
    Json.format[Subscribe].map(x => x: Event) or
    Json.format[ConnectionAlive].map(x => x: Event) or
    Json.format[ETHWalletTxEvent].map(x => x: Event)
  }

  implicit val implicitEventWrites = new Writes[Event] {
    def writes(event: Event): JsValue = {
      event match {
        case m: InEvent => Json.toJson(m)
        case m: OutEvent => Json.toJson(m)
        case m: Subscribe => Json.toJson(m)
        case m: ConnectionAlive => Json.toJson(m)
        case m: ETHWalletTxEvent => Json.toJson(m)
        case _ => Json.obj("error" -> "wrong Json")
      }
    }
  }
}