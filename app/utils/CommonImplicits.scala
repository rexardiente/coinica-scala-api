package utils

import java.time.Instant
import java.util.UUID
import play.api.libs.json._
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
	  	if (tx.id == JsNull) 
	  		Json.obj("input" -> tx.input)
	  	else
	  		Json.obj("id" -> tx.id, "input" -> tx.input)
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
	
	// EOSIO Tables..
	implicit val implicitGQCharacterPrevMatchDataReads: Reads[GQCharacterPrevMatchData] = new Reads[GQCharacterPrevMatchData] {
		override def reads(js: JsValue): JsResult[GQCharacterPrevMatchData] = js match {
			case json: JsValue => {
				try {
					JsSuccess(GQCharacterPrevMatchData(
						(json \ "enemy").as[String],
						(json \ "enemy_id").as[String],
						(json \ "time_executed").as[String].toLong,
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
						(json \ "hitpoints").as[Int],
						(json \ "ghost_class").as[Int],
						(json \ "ghost_level").as[Int],
						(json \ "status").as[Int],
						(json \ "attack").as[Int],
						(json \ "defense").as[Int],
						(json \ "speed").as[Int],
						(json \ "luck").as[Int],
						(json \ "prize").as[String],
						(json \ "battle_limit").as[Int],
						(json \ "battle_count").as[Int],
						(json \ "last_match").asOpt[String].map(_.toLong).getOrElse(0),
						(json \ "match_history").as[Seq[GQCharacterPrevMatch]]))
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
		"hitpoints" -> tx.hitpoints,
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
		"match_history" -> tx.match_history)
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
						(json \ "hitpoints").as[Int],
						(json \ "ghost_class").as[Int],
						(json \ "ghost_level").as[Int],
						(json \ "status").as[Int],
						(json \ "attack").as[Int],
						(json \ "defense").as[Int],
						(json \ "speed").as[Int],
						(json \ "luck").as[Int],
						(json \ "prize").as[String],
						(json \ "battle_limit").as[Int],
						(json \ "battle_count").as[Int],
						(json \ "last_match").as[Long]))
				} catch {
					case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
				}
			}
			case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
		}
	}
	implicit val implicitGQCharacterDataWrites = new Writes[GQCharacterData] {
	  def writes(tx: GQCharacterData): JsValue = Json.obj(
		"key" -> tx.characterID,
		"owner" -> tx.owner,
		"character_life" -> tx.character_life,
		"initial_hp" -> tx.initial_hp,
		"hitpoints" -> tx.hitpoints,
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
		"last_match" -> tx.last_match)
		// "match_history" -> tx.match_history)
	}
	implicit def implGQCharacterGameHistory = Json.format[GQCharacterGameHistory]
	implicit def implGQCharacterDataHistory = Json.format[GQCharacterDataHistory]
	
}