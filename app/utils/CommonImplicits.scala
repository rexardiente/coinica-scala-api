package utils

import java.time.Instant
import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.domain._
import models.domain.eosio._

trait CommonImplicits {
	// Common
	implicit def dateTimeStringToUnix(s: String): Option[Instant] =
    try {
      Some(Instant.parse(s))
    } catch {
      case _: Throwable => None
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
	implicit val implicitTxReads: Reads[Transaction] = new Reads[Transaction] {
		override def reads(js: JsValue): JsResult[Transaction] = js match {
			case json: JsValue => {
				try {
					JsSuccess(Transaction(
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
	implicit val implicitTxWrites = new Writes[Transaction] {
	  def writes(tx: Transaction): JsValue = Json.obj(
		"id" -> tx.id,
		"trace_id" -> tx.traceId,
		"block_num" -> tx.blockNum,
		"block_timestamp" -> Instant.ofEpochSecond(tx.blockTimestamp),
		"trace" -> tx.trace)
	}

	// EOSIO Tables..
	implicit val implicitGQCharacterPrevMatchDataReads: Reads[GQCharacterPrevMatchData] = new Reads[GQCharacterPrevMatchData] {
		override def reads(js: JsValue): JsResult[GQCharacterPrevMatchData] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterPrevMatchData(
						(json \ "enemy").as[String],
						(json \ "enemy_id").as[String],
						(json \ "time_executed").asOpt[String].map(_.slice(0, 10).toLong).getOrElse(0),
						(json \ "gameplay_log").as[List[String]],
						(if ((json \ "isWin").as[Int] > 0) true else false)
					))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitGQCharacterPrevMatchDataWrites = new Writes[GQCharacterPrevMatchData] {
	  def writes(tx: GQCharacterPrevMatchData): JsValue = Json.obj(
		"enemy" -> tx.enemy,
		"enemy_id" -> tx.enemy_id,
		"time_executed" -> tx.time_executed,
		"gameplay_log" -> tx.gameplay_log,
		"isWin" -> tx.isWin)
	}

	implicit def implGQCharacterPrevMatch = Json.format[GQCharacterPrevMatch]
	implicit val implicitGQCharacterInfoReads: Reads[GQCharacterInfo] = new Reads[GQCharacterInfo] {
		override def reads(js: JsValue): JsResult[GQCharacterInfo] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterInfo(
						(json \ "owner").as[String],
						(json \ "character_life").as[Int],
						(json \ "initial_hp").as[Int],
						(json \ "ghost_class").as[Int],
						(json \ "ghost_level").as[Int],
						(json \ "status").as[Int],
						(json \ "attack").as[Int],
						(json \ "defense").as[Int],
						(json \ "speed").as[Int],
						(json \ "luck").as[Int],
						{
							val prize: String = (json \ "prize").as[String]
							prize.splitAt(prize.size - 4)._1.toDoubleOption.getOrElse(0D)
						},
						(json \ "battle_limit").as[Int],
						(json \ "battle_count").as[Int],
						(json \ "last_match").asOpt[String].map(_.slice(0, 10).toLong).getOrElse(0),
						(json \ "match_history").as[Seq[GQCharacterPrevMatch]],
						(json \ "created_at").asOpt[String].map(_.slice(0, 10).toLong).getOrElse(0)))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitGQCharacterInfoWrites = new Writes[GQCharacterInfo] {
	  def writes(tx: GQCharacterInfo): JsValue = Json.obj(
		"owner" -> tx.owner,
		"character_life" -> tx.character_life,
		"initial_hp" -> tx.initial_hp,
		"ghost_class" -> tx.ghost_class,
		"ghost_level" -> tx.ghost_level,
		"status" -> tx.status,
		"attack" -> tx.attack,
		"defense" -> tx.defense,
		"speed" -> tx.speed,
		"luck" -> tx.luck,
		"prize" -> tx.prize,
		"battle_limit" -> tx.battle_limit,
		"battle_count" -> tx.battle_count,
		"last_match" -> tx.last_match,
		"match_history" -> tx.match_history,
		"created_at" -> tx.created_at)
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

	implicit val implicitGQCharacterDataReads: Reads[GQCharacterData] = new Reads[GQCharacterData] {
		override def reads(js: JsValue): JsResult[GQCharacterData] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterData(
						(json \ "key").as[String],
						(json \ "owner").as[String],
						(json \ "character_life").as[Int],
						(json \ "initial_hp").as[Int],
						(json \ "ghost_class").as[Int],
						(json \ "ghost_level").as[Int],
						(json \ "status").as[Int],
						(json \ "attack").as[Int],
						(json \ "defense").as[Int],
						(json \ "speed").as[Int],
						(json \ "luck").as[Int],
						(json \ "prize").as[Double],
						(json \ "battle_limit").as[Int],
						(json \ "battle_count").as[Int],
						(json \ "last_match").as[Long],
						(json \ "created_at").as[Long]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitGQCharacterDataWrites = new Writes[GQCharacterData] {
	  def writes(tx: GQCharacterData): JsValue = Json.obj(
		"key" -> tx.id,
		"owner" -> tx.owner,
		"character_life" -> tx.character_life,
		"initial_hp" -> tx.initial_hp,
		"ghost_class" -> tx.ghost_class,
		"ghost_level" -> tx.ghost_level,
		"status" -> tx.status,
		"attack" -> tx.attack,
		"defense" -> tx.defense,
		"speed" -> tx.speed,
		"luck" -> tx.luck,
		"prize" -> tx.prize,
		"battle_limit" -> tx.battle_limit,
		"battle_count" -> tx.battle_count,
		"last_match" -> tx.last_match,
		"created_at" -> tx.created_at)
		// "match_history" -> tx.match_history)
	}
	implicit def implGQGameStatus = Json.format[GQGameStatus]
	implicit val implicitGQCharacterGameHistoryReads: Reads[GQCharacterGameHistory] = new Reads[GQCharacterGameHistory] {
		override def reads(js: JsValue): JsResult[GQCharacterGameHistory] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterGameHistory(
						(json \ "game_id").as[String],
						(json \ "player1").as[String],
						(json \ "player1_id").as[String],
						(json \ "player2").as[String],
						(json \ "player2_id").as[String],
						(json \ "time_executed").as[Long],
						(json \ "gameplay_log").as[List[String]],
						(json \ "status").as[List[GQGameStatus]]))
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
		"player1" -> tx.player1,
		"player1_id" -> tx.player1ID,
		"player2" -> tx.player2,
		"player2_id" -> tx.player2ID,
		"time_executed" -> tx.timeExecuted,
		"gameplay_log" -> tx.log,
		"status" -> tx.status)
	}

	implicit def implGQCharacterDataHistory = Json.format[GQCharacterDataHistory]

	implicit val implicitGQCharacterDataHistoryLogsReads: Reads[GQCharacterDataHistoryLogs] = new Reads[GQCharacterDataHistoryLogs] {
		override def reads(js: JsValue): JsResult[GQCharacterDataHistoryLogs] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterDataHistoryLogs(
						(json \ "game_id").as[String],
						(json \ "is_win").as[List[GQGameStatus]],
						(json \ "time_executed").as[Long],
						(json \ "logs").as[List[String]]))
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
		"time_executed" -> tx.timeExecuted,
		"logs" -> tx.logs)
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

	implicit val implicitInEventMessageReads: Reads[InEventMessage] = {
    Json.format[GQCharacterCreated].map(x => x: InEventMessage) or
    Json.format[GQBattleTime].map(x => x: InEventMessage) or
    Json.format[VIPWSRequest].map(x => x: InEventMessage)
  }

  implicit val implicitInEventMessageWrites = new Writes[InEventMessage] {
    def writes(event: InEventMessage): JsValue = {
      event match {
        case m: GQCharacterCreated => Json.toJson(m)
        case m: GQBattleTime => Json.toJson(m)
        case m: VIPWSRequest => Json.toJson(m)
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
					JsSuccess(InEvent((json \ "id").as[JsString], (json \ "input").as[JsValue]))
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
	implicit val implicitEventReads: Reads[Event] = {
		Json.format[InEvent].map(x => x: Event) or
    Json.format[OutEvent].map(x => x: Event) or
    Json.format[Subscribe].map(x => x: Event) or
    Json.format[ConnectionAlive].map(x => x: Event)
  }

  implicit val implicitEventWrites = new Writes[Event] {
    def writes(event: Event): JsValue = {
      event match {
        case m: InEvent => Json.toJson(m)
        case m: OutEvent => Json.toJson(m)
        case m: Subscribe => Json.toJson(m)
        case m: ConnectionAlive => Json.toJson(m)
        case _ => Json.obj("error" -> "wrong Json")
      }
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
	implicit val implicitTransactionTypeReads: Reads[TransactionType] = {
	  Json.format[GameType].map(x => x: TransactionType) or
	  Json.format[PaymentType].map(x => x: TransactionType)
	}

	implicit val implicitTransactionTypeWrites = new Writes[TransactionType] {
	  def writes(event: TransactionType): JsValue = {
	    event match {
	      case m: GameType => Json.toJson(m)
	      case m: PaymentType => Json.toJson(m)
	      case _ => Json.obj("error" -> "wrong Json")
	    }
	  }
	}
}